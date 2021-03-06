/*
 * Copyright(c) 2008 Aisin Comcruise
 *  All Rights Reserved
 */
package org.sapid.checker.popup.actions;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.sapid.checker.cx.command.Makefile;
import org.sapid.checker.eclipse.progress.CreateSDBProgress;

/**
 * 「SDB を作成」のアクションデリゲータ<br>
 * Makefile の右クリックメニューと紐付けされる
 * @author Toshinori OUSKA
 */
public class CreateSDB implements IObjectActionDelegate {
    private IResource selectedItem = null;

    public void setActivePart(IAction action, IWorkbenchPart targetPart) {
    }

    public void run(IAction action) {
        String projectRealPath = "";
        String makefile = "Makefile";
        if (selectedItem instanceof IFile) {
            projectRealPath = selectedItem.getProject().getLocation().toFile()
                    .getAbsolutePath();
            makefile = selectedItem.getName().toString();
        } else if (selectedItem instanceof IProject) {
            projectRealPath = selectedItem.getLocation().toFile()
                    .getAbsolutePath();
        }

        try {
            if (!new Makefile(projectRealPath + File.separator + makefile)
                    .isContainedCCMacro()) {
                MessageDialog.openError(new Shell(), "Error in Sapid",
                        "Makefile にマクロ CC が定義されていません。\n\n" + projectRealPath
                                + File.separator + makefile);
                return;
            }
        } catch (FileNotFoundException e1) {
            MessageDialog.openError(new Shell(), "Error in Sapid",
                    "Makefile が見つかりません。\n\n" + projectRealPath + File.separator
                            + makefile);
            return;
        }

        ProgressMonitorDialog dialog = new ProgressMonitorDialog(new Shell());
        try {
            dialog.run(true, true, new CreateSDBProgress(projectRealPath,
                    makefile, dialog));
        } catch (InvocationTargetException e) {
            // 末尾10行だけを表示
            List<String> list = Arrays.asList(e.getCause().toString().split(
                    "\n"));
            if (list.size() > 10) {
                list = list.subList(list.size() - 10, list.size());
            }
            MessageDialog.openError(null, "Error in Sapid", joinArray(list,
                    "\n"));
        } catch (InterruptedException e) {
            // e.printStackTrace();
        }
    }

    /**
     * ruby の Array#join 風
     * @param list
     * @param sp
     * @return
     */
    private String joinArray(List<?> list, String sp) {
        StringBuffer buffer = new StringBuffer();
        for (Iterator<?> itr = list.iterator(); itr.hasNext();) {
            buffer.append(itr.next().toString());
            buffer.append(sp);
        }
        return buffer.toString();
    }

    public void selectionChanged(IAction action, ISelection selection) {
        if (selection instanceof StructuredSelection) {
            StructuredSelection ss = (StructuredSelection) selection;
            Object obj = ss.getFirstElement();
            if (obj instanceof IFile) {
                selectedItem = (IFile) obj;
            } else if (obj instanceof IProject) {
                selectedItem = (IProject) obj;
            }
        }
    }

}
