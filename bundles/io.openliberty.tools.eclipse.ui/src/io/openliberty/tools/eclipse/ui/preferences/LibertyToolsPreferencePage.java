/*******************************************************************************
* Copyright (c) 2022 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     IBM Corporation - initial implementation
*******************************************************************************/
package io.openliberty.tools.eclipse.ui.preferences;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import io.openliberty.tools.eclipse.utils.LibertyPrefDirectoryFieldEditor;
import io.openliberty.tools.eclipse.utils.Utils;

public class LibertyToolsPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    LibertyPrefDirectoryFieldEditor mvnInstallFE;
    LibertyPrefDirectoryFieldEditor gradleInstallFE;
    IntegerFieldEditor debugIntFE;

    public LibertyToolsPreferencePage() {

        super(GRID);
    }

    @Override
    public void createFieldEditors() {

        mvnInstallFE = new LibertyPrefDirectoryFieldEditor("MVNPATH", "&Maven Install Location:",
                getFieldEditorParent());
        gradleInstallFE = new LibertyPrefDirectoryFieldEditor("GRADLEPATH", "&Gradle Install Location:",
                getFieldEditorParent());
        debugIntFE = new IntegerFieldEditor("DEBUG_TIMEOUT", "&Debug Timeout Value (in seconds):",
                getFieldEditorParent());
        debugIntFE.setValidRange(0, 9000);

        addField(mvnInstallFE);
        addField(gradleInstallFE);
        addField(debugIntFE);
    }

    @Override
    public void init(IWorkbench workbench) {
        // second parameter is typically the plug-in id
        setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, "io.openliberty.tools.eclipse.ui"));
        setDescription(
                "Use the Browse buttons to specify the Maven and Gradle installation locations to be used for starting the application in dev mode, which will be used if no mvnw/gradlew wrapper is found.");
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        // Will be called upon any preference update
        // Must check the validation of both fields in order to output
        // the correct error message if needed
        //
        boolean isMvn;
        boolean installMvnLocValid = false;
        boolean installGradleLocValid = false;
        boolean debugTimeoutValid = false;

        String eventProp = event.getProperty();
        if (event.getProperty().equals("field_editor_value")) {
            // field for which validation is required
            if (event.getSource() == mvnInstallFE) {
                // validate mvn loc
                isMvn = true;
                installMvnLocValid = doValidation(isMvn, mvnInstallFE.getStringValue());
                installGradleLocValid = doValidation(false, gradleInstallFE.getStringValue());
                debugTimeoutValid = doValidation(Integer.parseInt(debugIntFE.getStringValue()));
            } else {
                // validate gradle loc
                isMvn = false;
                installGradleLocValid = doValidation(isMvn, gradleInstallFE.getStringValue());
                installMvnLocValid = doValidation(true, mvnInstallFE.getStringValue());
                debugTimeoutValid = doValidation(Integer.parseInt(debugIntFE.getStringValue()));
            }

            if (event.getSource() == debugIntFE) {
                debugTimeoutValid = doValidation(Integer.parseInt(debugIntFE.getStringValue()));
            }

            if (installMvnLocValid && installGradleLocValid && debugTimeoutValid) {
                setValid(true);
                setErrorMessage(null);
                super.performApply();
                super.propertyChange(event);
            }
            // validation fails
            else {
                setValid(false);
                if (!installMvnLocValid && !installGradleLocValid) {
                    setErrorMessage("Install locations must contain mvn and gradle executables");
                } else if (!installMvnLocValid && installGradleLocValid) {
                    setErrorMessage("Install location must contain a mvn executable");
                } else if (installMvnLocValid && !installGradleLocValid) {
                    setErrorMessage("Install location must contain a gradle executable");
                } else if (!debugTimeoutValid) {
                    setErrorMessage("Debug Timeout Value must be between 0 and 9000");
                } 
            }
        }
    }

    private boolean doValidation(boolean ismvn, String installLoc) {
        if (installLoc.equals("")) {
            // an empty field is ok
            return true;
        } else {
            if (ismvn) {
                Path mvnCmd = Paths.get(installLoc + File.separator, Utils.isWindows() ? "mvn.cmd" : "mvn");
                return Files.exists(mvnCmd);
            } else {
                Path mvnCmd = Paths.get(installLoc + File.separator, Utils.isWindows() ? "gradle.bat" : "gradle");
                return Files.exists(mvnCmd);
            }
        }
    }

    private boolean doValidation(int timeoutVal) {
        if (timeoutVal < 0 || timeoutVal > 9000) {
            return false;
        } else {
            return true;
        }
    }
}
