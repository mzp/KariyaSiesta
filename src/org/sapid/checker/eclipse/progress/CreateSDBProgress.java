/*
 * Copyright(c) 2008 Aisin Comcruise
 *  All Rights Reserved
 */
package org.sapid.checker.eclipse.progress;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.regex.Matcher;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.sapid.checker.cx.command.Command;
import org.sapid.checker.cx.command.CommandOutput;
import org.sapid.checker.eclipse.CheckerActivator;
import org.sapid.checker.eclipse.Messages;
import org.sapid.parser.common.ParseException;

/**
 * SDB を作るための IRunnableWithProgress
 * @author Toshinori OSUKA
 */
public class CreateSDBProgress implements IRunnableWithProgress {
    private String curDir;
    private String makefile = "Makefile";
    private ProgressMonitorDialog dialog;

    public CreateSDBProgress(String curDir, String makefile,
            ProgressMonitorDialog dialog) {
        super();
        this.curDir = curDir;
        if (makefile != null) {
            this.makefile = makefile;
        }
        this.dialog = dialog;
    }

    public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
        final int TASK_AMOUNT = 3;
        monitor.beginTask("Chcker", TASK_AMOUNT);

        String sapidDest = CheckerActivator.getDefault().getPreferenceStore()
                .getString("SAPID_DEST");
        if (!new File(sapidDest).exists()) {
            throw new InvocationTargetException(new FileNotFoundException(
                    Messages.getString("CreateSDBProgress.0")));
        }
        try {
	    String sapidDestUnix = sapidDest;
	    if (System.getProperty("os.name").contains("Windows")) {
		// cygpath
		monitor.setTaskName("cygpath");
		sapidDestUnix = kickCygPath(monitor, sapidDest);
	    } else {
		monitor.setTaskName("path");
	    }
	    monitor.worked(1);

            if (monitor.isCanceled()) {
                throw new InterruptedException(Messages
                        .getString("CheckWithProgress.CANCELD"));
            }

            // sdb4
            ProgressDialogOutput output = new ProgressDialogOutput(monitor);
            int exitValue = kickSDB4(monitor, sapidDestUnix, output);
            if (exitValue != 0) {
                throw new InvocationTargetException(new ParseException(output
                        .getStored()));
            }
            output.setStored("");
            if (monitor.isCanceled()) {
                throw new InterruptedException(Messages
                        .getString("CheckWithProgress.CANCELD"));
            }
            dialog.setCancelable(false);
            // spdMkCXModel
            exitValue = kickSpdMkCXModel(monitor, sapidDestUnix, output);
            if (exitValue != 0) {
                throw new InvocationTargetException(new ParseException(output
                        .getStored()));
            }
        } catch (IOException e) {
            throw new InvocationTargetException(e);
        }

        monitor.done();
    }

    /**
     * Windows パスの SAPID_DEST から Unix パスを取得するために CygPath を kick する
     * @param monitor
     * @param sapidDest
     * @return
     * @throws IOException
     */
    private String kickCygPath(IProgressMonitor monitor, String sapidDest)
            throws IOException {
        ProgressDialogOutput output = new ProgressDialogOutput(monitor);
	String cmd = "cygpath -u \"" + sapidDest + "\"";
        new Command(cmd, curDir).run(output);
        String sapidDestUnix = output.getStored().trim();
        return sapidDestUnix;
    }

    /**
     * SDB4 を Kick する
     * @param monitor
     * @param sapidDestUnix
     * @param output
     * @return
     * @throws IOException
     */
    private int kickSDB4(IProgressMonitor monitor, String sapidDestUnix,
            ProgressDialogOutput output) throws IOException {
        monitor.setTaskName("sdb4");
        String cmd = "bash -c \"source " + sapidDestUnix
                + "/lib/SetUp.sh;make -B ";
	cmd = cmd.replaceAll("//", "/");
        if ("Makefile".equalsIgnoreCase(makefile)) {
            cmd += "-f " + makefile;
        }
        cmd += " CC=sdb4\"";
        int exitValue = new Command(cmd, curDir).run(output);
        CheckerActivator.log(output.getStored());
        monitor.worked(1);
        return exitValue;
    }

    /**
     * spdMkCXModel を Kick する
     * @param monitor
     * @param sapidDestUnix
     * @param output
     * @return
     * @throws IOException
     */
    private int kickSpdMkCXModel(IProgressMonitor monitor,
            String sapidDestUnix, ProgressDialogOutput output)
            throws IOException {
        int exitValue;
        monitor.setTaskName("spdMkCXModel");
        String sdb_spec = org.sapid.checker.cx.Messages.getString("SDB_SPEC");
        // SDB\SPEC => SDB/SPEC
        sdb_spec = sdb_spec.replaceAll("\\\\", Matcher.quoteReplacement("/"));
        String cmd = "bash -c \"source " + sapidDestUnix
                + "/lib/SetUp.sh;mkFlowView;spdMkCXModel -f -t -v -sdbD " + sdb_spec + "\"";
        exitValue = new Command(cmd, curDir).run(output);
        CheckerActivator.log(output.getStored());
        monitor.worked(1);
        return exitValue;
    }

    /**
     * Progress Monitor に状況を表示する CommandOutput
     * @author Toshinori OSUKA
     */
    public class ProgressDialogOutput implements CommandOutput {
        IProgressMonitor monitor;
        String stored = "";

        public ProgressDialogOutput(IProgressMonitor monitor) {
            this.monitor = monitor;
        }

        public String hook(String buffer) {
            monitor.setTaskName(buffer);
            // CheckerActivator.log(buffer);
            stored += buffer + "\n";
            return buffer;
        }

        public String getStored() {
            return stored;
        }

        public void setStored(String stored) {
            this.stored = stored;
        }
    }
}
