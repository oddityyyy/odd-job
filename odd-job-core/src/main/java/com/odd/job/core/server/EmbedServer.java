package com.odd.job.core.server;

import com.odd.job.core.biz.ExecutorBiz;
import com.odd.job.core.biz.impl.ExecutorBizImpl;
import com.odd.job.core.biz.model.IdleBeatParam;
import com.odd.job.core.biz.model.ReturnT;
import com.odd.job.core.util.GsonTool;
import com.odd.job.core.util.OddJobRemotingUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Copy from : https://github.com/oddityyyy/odd-rpc
 *
 * @author oddity
 * @create 2023-12-08 16:14
 */
public class EmbedServer {

    private static final Logger logger = LoggerFactory.getLogger(EmbedServer.class);

    private ExecutorBiz executorBiz;
    private Thread thread;

    public void start(final String address, final int port, final String appname, final String accessToken){
        executorBiz = new ExecutorBizImpl();
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // param
                NioEventLoopGroup bossGroup = new NioEventLoopGroup();
                NioEventLoopGroup workerGroup = new NioEventLoopGroup();
                ThreadPoolExecutor bizThreadPool = new ThreadPoolExecutor(
                        0,
                        200,
                        60L,
                        TimeUnit.SECONDS,
                        new LinkedBlockingQueue<Runnable>(2000),
                        new ThreadFactory() {
                            @Override
                            public Thread newThread(Runnable r) {
                                return new Thread(r, "odd-job, EmbedServer bizThreadPool-" + r.hashCode());
                            }
                        },
                        new RejectedExecutionHandler() {
                            @Override
                            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                                throw new RuntimeException("odd-job, EmbedServer bizThreadPool is EXHAUSTED!");
                            }
                        });

                try {
                    // start server
                    ServerBootstrap bootstrap = new ServerBootstrap();
                    bootstrap.group(bossGroup, workerGroup)
                            .channel(NioServerSocketChannel.class)
                            .childHandler(new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel channel) throws Exception {
                                    channel.pipeline()
                                            .addLast(new IdleStateHandler(0, 0, 30 * 3, TimeUnit.SECONDS))  // beat 3N, close if idle
                                            .addLast(new HttpServerCodec())  // （双向处理器）添加 HTTP 服务器编解码器，用于将 HTTP 请求和响应进行编解码。
                                            .addLast(new HttpObjectAggregator(5 * 1024 * 1024)) // (入站) merge request & response to FULL 聚合 HTTP 消息的处理器，设置了最大聚合内容长度为 5MB（5 * 1024 * 1024）。它将 HTTP 消息的多个部分聚合成一个完整的 FullHttpRequest 或 FullHttpResponse。
                                            .addLast(new EmbedHttpServerHandler(executorBiz, accessToken, bizThreadPool)); // 入站处理器
                                }
                            })
                            .childOption(ChannelOption.SO_KEEPALIVE, true);

                    // bind
                    ChannelFuture future = bootstrap.bind(port).sync();

                    logger.info(">>>>>>>>>>> odd-job remoting server start success, nettype = {}, port = {}", EmbedServer.class, port);

                    // start registry
                    startRegistry(appname, address);

                    // wait util stop
                    future.channel().closeFuture().sync();

                } catch (InterruptedException e) {
                    logger.info(">>>>>>>>>>> odd-job remoting server stop.");
                } catch (Exception e){
                    logger.error(">>>>>>>>>>> odd-job remoting server error.", e);
                } finally {
                    // stop
                    try {
                        workerGroup.shutdownGracefully();
                        bossGroup.shutdownGracefully();
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });
        thread.setDaemon(true);  // daemon, service jvm, user thread leave >>> daemon leave >>> jvm leave
        thread.start();
    }

    public void stop() throws Exception {
        // destroy server thread
        if (thread != null && thread.isAlive()){
            thread.interrupt();
        }

        // stop registry
        stopRegistry();
    }


    // ---------------------- registry ----------------------

    /**
     * 入站处理器
     *
     * netty_http
     * Copy from : https://github.com/oddityyyy/odd-rpc
     */
    public static class EmbedHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        private static final Logger logger = LoggerFactory.getLogger(EmbedHttpServerHandler.class);

        private ExecutorBiz executorBiz;
        private String accessToken;
        private ThreadPoolExecutor bizThreadPool;

        public EmbedHttpServerHandler(ExecutorBiz executorBiz, String accessToken, ThreadPoolExecutor bizThreadPool) {
            this.executorBiz = executorBiz;
            this.accessToken = accessToken;
            this.bizThreadPool = bizThreadPool;
        }

        @Override
        protected void channelRead0(final ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
            // request parse
            String requestData = msg.content().toString(CharsetUtil.UTF_8);
            String uri = msg.uri();
            HttpMethod httpMethod = msg.method();
            boolean keepAlive = HttpUtil.isKeepAlive(msg);
            String accessTokenReq = msg.headers().get(OddJobRemotingUtil.ODD_JOB_ACCESS_TOKEN);

            // invoke
            bizThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    // do invoke
                    process(httpMethod, uri, requestData, accessTokenReq);
                }
            });
        }

        private Object process(HttpMethod httpMethod, String uri, String requestData, String accessTokenReq){
            // valid
            if (HttpMethod.POST != httpMethod) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, HttpMethod not support.");
            }
            if (uri == null || uri.trim().length() == 0){
                return new ReturnT<String>(ReturnT.FAIL_CODE, "invalid request, uri-mapping empty.");
            }
            if (accessToken != null && accessToken.trim().length() > 0 && !accessToken.equals(accessTokenReq)){
                return new ReturnT<String>(ReturnT.FAIL_CODE, "The access token is wrong.");
            }

            // service mapping

            switch (uri){
                case "/beat":
                    return executorBiz.beat();
                case "/idleBeat":
                    IdleBeatParam idleBeatParam = GsonTool.fromJson(requestData, IdleBeatParam.class);
                    return executorBiz.idleBeat(idleBeatParam);

            }
        }
    }
}
