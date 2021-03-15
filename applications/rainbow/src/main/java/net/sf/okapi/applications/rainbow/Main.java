/*===========================================================================
  Copyright (C) 2008-2009 by the Okapi Framework contributors
-----------------------------------------------------------------------------
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
===========================================================================*/

package net.sf.okapi.applications.rainbow;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class Main {

	public static void main (String args[]) {
		Display dispMain = null;
		Shell shlMain = null;
		int exitCode = 0;

		try {
			dispMain = new Display();
			shlMain = new Shell(dispMain);
		} catch ( Throwable e ) {
			e.printStackTrace();
			exitCode = 1;
		}

		if ( args.length < 2 && shlMain != null) {
			// Normal mode
			String projectFile = null;
			if ( args.length == 1 ) {
				projectFile = args[0];
			}
			MainForm mf = new MainForm(shlMain, projectFile);
			shlMain.open();
			mf.run();
		}
		else { // Command line mode
			CommandLine cmd = new CommandLine();
			exitCode = cmd.execute(shlMain, args);
		}

		if ( dispMain != null ) dispMain.dispose();

		if ( exitCode > 0 ) {
			System.exit(exitCode);
		}
	}

}
