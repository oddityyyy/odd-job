package com.odd.job.core.handler.impl;

import com.odd.job.core.context.OddJobContext;
import com.odd.job.core.context.OddJobHelper;
import com.odd.job.core.glue.GlueTypeEnum;
import com.odd.job.core.handler.IJobHandler;
import com.odd.job.core.log.OddJobFileAppender;
import com.odd.job.core.util.ScriptUtil;


import java.io.File;

/**
 * @author oddity
 * @create 2023-12-05 22:01
 */
public class ScriptJobHandler extends IJobHandler {

    private int jobId;
    private long glueUpdatetime;
    private String gluesource;
    private GlueTypeEnum glueType;

    public ScriptJobHandler(int jobId, long glueUpdatetime, String gluesource, GlueTypeEnum glueType) {
        this.jobId = jobId;
        this.glueUpdatetime = glueUpdatetime;
        this.gluesource = gluesource;
        this.glueType = glueType;

        // clean old script file
        File glueSrcPath = new File(OddJobFileAppender.getGlueSrcPath());
        if (glueSrcPath.exists()){
            File[] glueSrcFileList = glueSrcPath.listFiles();
            if (glueSrcFileList != null && glueSrcFileList.length > 0){
                for (File glueSrcFileItem : glueSrcFileList){
                    if (glueSrcFileItem.getName().startsWith(String.valueOf(jobId) + "_")){
                        glueSrcFileItem.delete();
                    }
                }
            }
        }
    }

    public long getGlueUpdatetime() {
        return glueUpdatetime;
    }

    @Override
    public void execute() throws Exception {

        if (!glueType.isScript()){
            OddJobHelper.handleFail("glueType["+ glueType +"] invalid.");
            return;
        }

        // cmd
        String cmd = glueType.getCmd();

        // make script file
        String scriptFileName = OddJobFileAppender.getGlueSrcPath()
                .concat(File.separator)
                .concat(String.valueOf(jobId))
                .concat("_")
                .concat(String.valueOf(glueUpdatetime))
                .concat(glueType.getSuffix());
        File scriptFile = new File(scriptFileName);
        if (!scriptFile.exists()){
            ScriptUtil.markScriptFile(scriptFileName, gluesource);
        }

        // log file
        String logFileName = OddJobContext.getOddJobContext().getJobLogFileName();

        // script params: 0=param 1=分片序号 2=分片总数
        String[] scriptParams = new String[3];
        scriptParams[0] = OddJobHelper.getJobParam();
        scriptParams[1] = String.valueOf(OddJobContext.getOddJobContext().getShardIndex());
        scriptParams[2] = String.valueOf(OddJobContext.getOddJobContext().getShardTotal());

        // invoke
        OddJobHelper.log("----------- script file:"+ scriptFileName +" -----------");
        int exitValue = ScriptUtil.execToFile(cmd, scriptFileName, logFileName, scriptParams);

        if (exitValue == 0){
            OddJobHelper.handleSuccess();
            return;
        } else {
            OddJobHelper.handleFail("script exit value("+exitValue+") is failed");
            return;
        }
    }
}
