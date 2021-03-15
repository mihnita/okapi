/*===========================================================================
  Copyright (C) 2009-2017 by the Okapi Framework contributors
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
============================================================================*/

package net.sf.okapi.filters.xliff.ui;

import net.sf.okapi.common.EditorFor;
import net.sf.okapi.common.IContext;
import net.sf.okapi.common.IHelp;
import net.sf.okapi.common.IParameters;
import net.sf.okapi.common.IParametersEditor;
import net.sf.okapi.common.ui.Dialogs;
import net.sf.okapi.common.ui.OKCancelPanel;
import net.sf.okapi.common.ui.filters.InlineCodeFinderPanel;
import net.sf.okapi.filters.xliff.Parameters;
import static net.sf.okapi.filters.xliff.Parameters.SegmentationType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

@EditorFor(Parameters.class)
public class Editor implements IParametersEditor {

	private Shell shell;
	private boolean result = false;
	private OKCancelPanel pnlActions;
	private Parameters params;

	private InlineCodeFinderPanel pnlCodeFinder;
	private IHelp help;

	private Button chkFallBackToId;
	private Button chkIgnoreInputSegmentation;
	private Button chkAlwaysUseSegSource;
	private Button chkUseSdlXliffWriter;
	private Button chkEscapeGt;
	private Button chkAddTargetLanguage;
	private Button chkOverrideTargetLanguage;
	private Button chkAllowEmptyTargets;
	private List listOutputSegmentationType;
	private Button chkAddAltTrans;
	private Button chkIncludeITS;
	private Button chkBalanceCodes;
	private Button chkPreserveSpaceByDefault;
	private Button chkInlineCdata;
	private Button chkSkipNoMrkSegSource;
	private Button chkUseCustomParser;
	private Text edSubfilterId;
	private Button chkUseCodeFinder;
	private Text edCustomParserClass;

	private Button chkUseIwsXliffWriter;
	private Button chkIwsBlockFinished;
	private Text edTransStatusValue;
	private Button chkIwsRemoveTmOrigin;
	private Text edTransTypeValue;
	private Button chkIwsBlockLockStatus;
	private Button chkIwsBlockTmScore;
	private Text edIwsBlockTmScoreValue;
	private Button chkIwsIncludeMultipleExact;
	private Button chkIwsBlockMultipleExact;



	private static EnumMap<SegmentationType, String> segTypeMap = new EnumMap<>(
			SegmentationType.class);
	static {
		segTypeMap.put(SegmentationType.ORIGINAL, Res.getString("segType_original"));
		segTypeMap.put(SegmentationType.SEGMENTED, Res.getString("segType_always"));
		segTypeMap.put(SegmentationType.NOTSEGMENTED, Res.getString("segType_never"));
		segTypeMap.put(SegmentationType.ASNEEDED, Res.getString("segType_asNeeded"));
	}

	private static Optional<SegmentationType> getSegTypeForLabel(String label) {
		return segTypeMap.entrySet().stream()
				.filter(e -> e.getValue().equals(label)).findFirst()
				.map(Map.Entry::getKey);
	}

	public boolean edit(IParameters params, boolean readOnly,
			IContext context) {
		help = (IHelp) context.getObject("help");
		boolean bRes = false;
		shell = null;
		this.params = (Parameters) params;
		try {
			shell = new Shell((Shell) context.getObject("shell"),
					SWT.CLOSE | SWT.TITLE | SWT.RESIZE | SWT.APPLICATION_MODAL);
			create((Shell) context.getObject("shell"), readOnly);
			return showDialog();
		} catch (Exception E) {
			Dialogs.showError(shell, E.getLocalizedMessage(), null);
			bRes = false;
		} finally {
			// Dispose of the shell, but not of the display
			if (shell != null)
				shell.dispose();
		}
		return bRes;
	}

	public IParameters createParameters() {
		return new Parameters();
	}

	private void create(Shell parent, boolean readOnly) {
		shell.setText(Res.getString("EditorCaption"));
		if (parent != null)
			shell.setImage(parent.getImage());
		GridLayout layTmp = new GridLayout();
		layTmp.marginBottom = 0;
		layTmp.verticalSpacing = 0;
		shell.setLayout(layTmp);

		TabFolder tfTmp = new TabFolder(shell, SWT.NONE);
		GridData gdTmp = new GridData(GridData.FILL_BOTH);
		tfTmp.setLayoutData(gdTmp);

		// --- Options tab

		Composite cmpTmp = new Composite(tfTmp, SWT.NONE);
		layTmp = new GridLayout();
		cmpTmp.setLayout(layTmp);

		Group grpTmp = new Group(cmpTmp, SWT.NONE);
		layTmp = new GridLayout();
		grpTmp.setLayout(layTmp);
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		grpTmp.setLayoutData(gdTmp);

		chkFallBackToId = new Button(grpTmp, SWT.CHECK);
		chkFallBackToId.setText(Res.getString("fallBackToId"));
		chkIgnoreInputSegmentation = new Button(grpTmp, SWT.CHECK);
		chkIgnoreInputSegmentation
				.setText(Res.getString("ignoreInputSegmentation"));

		chkAlwaysUseSegSource = new Button(grpTmp, SWT.CHECK);
		chkAlwaysUseSegSource.setText(Res.getString("alwaysUseSegSource"));

		chkUseSdlXliffWriter = new Button(grpTmp, SWT.CHECK);
		chkUseSdlXliffWriter.setText(Res.getString("useSdlXliffWriter"));

		chkEscapeGt = new Button(grpTmp, SWT.CHECK);
		chkEscapeGt.setText(Res.getString("escapeGt"));

		chkAddTargetLanguage = new Button(grpTmp, SWT.CHECK);
		chkAddTargetLanguage.setText(Res.getString("addTargetLanguage"));

		chkOverrideTargetLanguage = new Button(grpTmp, SWT.CHECK);
		chkOverrideTargetLanguage
				.setText(Res.getString("overrideTargetLanguage"));

		chkAllowEmptyTargets = new Button(grpTmp, SWT.CHECK);
		chkAllowEmptyTargets.setText(Res.getString("allowEmptyTargets"));
		chkPreserveSpaceByDefault = new Button(grpTmp, SWT.CHECK);
		chkPreserveSpaceByDefault
				.setText(Res.getString("preserveSpaceByDefault"));

		chkInlineCdata = new Button(grpTmp, SWT.CHECK);
		chkInlineCdata.setText(Res.getString("inlineCdata"));

		chkSkipNoMrkSegSource = new Button(grpTmp, SWT.CHECK);
		chkSkipNoMrkSegSource.setText(Res.getString("skipNoMrkSegSource"));

		Label label = new Label(grpTmp, SWT.NONE);
		label.setText(Res.getString("outputSegmentationType"));
		listOutputSegmentationType = new List(grpTmp, SWT.BORDER);
		GridData gd_listOutputSegmentationType = new GridData(SWT.FILL,
				SWT.FILL, true, true, 1, 1);
		gd_listOutputSegmentationType.heightHint = 80;
		listOutputSegmentationType.setLayoutData(gd_listOutputSegmentationType);
		listOutputSegmentationType
				.setItems(segTypeMap.values().toArray(new String[]{}));

		chkIncludeITS = new Button(grpTmp, SWT.CHECK);
		chkIncludeITS.setText(Res.getString("includeITS"));

		chkBalanceCodes = new Button(grpTmp, SWT.CHECK);
		chkBalanceCodes.setText(Res.getString("balanceCodes"));

		chkAddAltTrans = new Button(grpTmp, SWT.CHECK);
		chkAddAltTrans.setText(Res.getString("addAltTrans"));

		chkUseCustomParser = new Button(grpTmp, SWT.CHECK);
		chkUseCustomParser.setText(Res.getString("useCustomParser"));
		label = new Label(grpTmp, SWT.NONE);
		label.setText(Res.getString("customParserClass"));
		edCustomParserClass = new Text(grpTmp, SWT.BORDER);
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		edCustomParserClass.setLayoutData(gdTmp);
		chkUseCustomParser.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				edCustomParserClass
						.setEnabled(chkUseCustomParser.getSelection());
			}
		});

		// IWS Options
		chkUseIwsXliffWriter = new Button(grpTmp, SWT.CHECK);
		chkUseIwsXliffWriter.setText(Res.getString("useIwsXliffWriter"));
		chkUseIwsXliffWriter.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				chkIwsBlockFinished
					.setEnabled(chkUseIwsXliffWriter.getSelection());
				edTransStatusValue
					.setEnabled(chkUseIwsXliffWriter.getSelection());
				edTransTypeValue
					.setEnabled(chkUseIwsXliffWriter.getSelection());
				chkIwsRemoveTmOrigin
					.setEnabled(chkUseIwsXliffWriter.getSelection());
				chkIwsBlockLockStatus
					.setEnabled(chkUseIwsXliffWriter.getSelection());
				chkIwsBlockTmScore
					.setEnabled(chkUseIwsXliffWriter.getSelection());
				edIwsBlockTmScoreValue
					.setEnabled(chkUseIwsXliffWriter.getSelection());
				chkIwsIncludeMultipleExact
					.setEnabled(chkUseIwsXliffWriter.getSelection());
				chkIwsBlockMultipleExact
					.setEnabled(chkUseIwsXliffWriter.getSelection());
			}
		});
		chkIwsBlockFinished = new Button(grpTmp, SWT.CHECK);
		chkIwsBlockFinished.setText(Res.getString("iwsBlockFinished"));
		label = new Label(grpTmp, SWT.NONE);
		label.setText(Res.getString("iwsTransStatusValue"));
		edTransStatusValue = new Text(grpTmp, SWT.BORDER);
		edTransStatusValue.setLayoutData(gdTmp);
		label = new Label(grpTmp, SWT.NONE);
		label.setText(Res.getString("iwsTransTypeValue"));
		edTransTypeValue = new Text(grpTmp, SWT.BORDER);
		edTransTypeValue.setLayoutData(gdTmp);
		chkIwsRemoveTmOrigin = new Button(grpTmp, SWT.CHECK);
		chkIwsRemoveTmOrigin.setText(Res.getString("iwsRemoveTmOrigin"));
		chkIwsBlockLockStatus = new Button(grpTmp, SWT.CHECK);
		chkIwsBlockLockStatus.setText(Res.getString("iwsBlockLockStatus"));
		chkIwsBlockTmScore = new Button(grpTmp, SWT.CHECK);
		chkIwsBlockTmScore.setText(Res.getString("iwsBlockTmScore"));
		label = new Label(grpTmp, SWT.NONE);
		label.setText(Res.getString("iwsBlockTmScoreValue"));
		chkIwsBlockTmScore.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				edIwsBlockTmScoreValue
					.setEnabled(chkIwsBlockTmScore.getSelection());
				chkIwsIncludeMultipleExact
					.setEnabled(chkIwsBlockTmScore.getSelection());
			}
		});
		edIwsBlockTmScoreValue = new Text(grpTmp, SWT.BORDER);
		edIwsBlockTmScoreValue.setLayoutData(gdTmp);
		chkIwsIncludeMultipleExact = new Button(grpTmp, SWT.CHECK);
		chkIwsIncludeMultipleExact.setText(Res.getString("iwsIncludeMultipleExact"));
		chkIwsBlockMultipleExact = new Button(grpTmp, SWT.CHECK);
		chkIwsBlockMultipleExact.setText(Res.getString("iwsBlockMultipleExact"));

		TabItem tiTmp = new TabItem(tfTmp, SWT.NONE);
		tiTmp.setText(Res.getString("tabOptions"));
		tiTmp.setControl(cmpTmp);

		// --- Inline tab

		cmpTmp = new Composite(tfTmp, SWT.NONE);
		layTmp = new GridLayout();
		cmpTmp.setLayout(layTmp);

		grpTmp = new Group(cmpTmp, SWT.NONE);
		grpTmp.setLayout(layTmp);
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		grpTmp.setLayoutData(gdTmp);

		label = new Label(grpTmp, SWT.NONE);
		label.setText(Res.getString("cdataSubfilter"));

		edSubfilterId = new Text(grpTmp, SWT.BORDER);
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		edSubfilterId.setLayoutData(gdTmp);

		chkUseCodeFinder = new Button(grpTmp, SWT.CHECK);
		chkUseCodeFinder.setText(Res.getString("codeFinder"));
		SelectionAdapter listener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateInlineCodes();
			};
		};
		chkUseCodeFinder.addSelectionListener(listener);
		chkUseCodeFinder.addSelectionListener(listener);

		pnlCodeFinder = new InlineCodeFinderPanel(grpTmp, SWT.NONE);
		pnlCodeFinder.setLayoutData(new GridData(GridData.FILL_BOTH));

		tiTmp = new TabItem(tfTmp, SWT.NONE);
		tiTmp.setText(Res.getString("tabEmbeddedContent"));
		tiTmp.setControl(cmpTmp);

		// --- Dialog-level buttons

		SelectionAdapter OKCancelActions = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				result = false;
				if (e.widget.getData().equals("h")) {
					if (help != null)
						help.showWiki("XLIFF Filter");
					return;
				}
				if (e.widget.getData().equals("o")) {
					if (!saveData())
						return;
					result = true;
				}
				shell.close();
			};
		};
		pnlActions = new OKCancelPanel(shell, SWT.NONE, OKCancelActions, true);
		gdTmp = new GridData(GridData.FILL_HORIZONTAL);
		pnlActions.setLayoutData(gdTmp);
		pnlActions.btOK.setEnabled(!readOnly);
		if (!readOnly) {
			shell.setDefaultButton(pnlActions.btOK);
		}

		shell.pack();
		Rectangle Rect = shell.getBounds();
		shell.setMinimumSize(Rect.width, Rect.height);
		Dialogs.centerWindow(shell, parent);
		setData();
	}

	private boolean showDialog() {
		shell.open();
		while (!shell.isDisposed()) {
			if (!shell.getDisplay().readAndDispatch())
				shell.getDisplay().sleep();
		}
		return result;
	}

	private void setData() {
		chkFallBackToId.setSelection(params.getFallbackToID());
		chkIgnoreInputSegmentation
				.setSelection(params.getIgnoreInputSegmentation());
		chkAlwaysUseSegSource.setSelection(params.isAlwaysUseSegSource());
		chkUseSdlXliffWriter.setSelection(params.isUseSdlXliffWriter());
		chkEscapeGt.setSelection(params.getEscapeGT());
		chkAddTargetLanguage.setSelection(params.getAddTargetLanguage());
		chkOverrideTargetLanguage
				.setSelection(params.getOverrideTargetLanguage());
		chkAllowEmptyTargets.setSelection(params.getAllowEmptyTargets());

		listOutputSegmentationType.setSelection(new String[]{
				segTypeMap.get(params.getOutputSegmentationType())});

		chkAddAltTrans.setSelection(params.getAddAltTrans());
		chkIncludeITS.setSelection(params.getIncludeIts());
		chkBalanceCodes.setSelection(params.getBalanceCodes());
		chkPreserveSpaceByDefault
				.setSelection(params.isPreserveSpaceByDefault());
		chkInlineCdata.setSelection(params.isInlineCdata());
		chkSkipNoMrkSegSource.setSelection(params.getSkipNoMrkSegSource());
		chkUseCustomParser.setSelection(params.getUseCustomParser());
		edCustomParserClass.setText(params.getFactoryClass() == null
				? ""
				: params.getFactoryClass());

		chkUseIwsXliffWriter.setSelection(params.isUseIwsXliffWriter());
		chkIwsBlockFinished.setSelection(params.isIwsBlockFinished());
		edTransStatusValue.setText(params.getIwsTransStatusValue() == null
				? ""
				: params.getIwsTransStatusValue());
		edTransTypeValue.setText(params.getIwsTransTypeValue() == null
			? ""
			: params.getIwsTransTypeValue());
		chkIwsRemoveTmOrigin.setSelection(params.isIwsRemoveTmOrigin());
		chkIwsBlockLockStatus.setSelection(params.isIwsBlockLockStatus());
		chkIwsBlockTmScore.setSelection(params.isIwsBlockTmScore());
		edIwsBlockTmScoreValue.setText(params.getIwsBlockTmScoreValue() == null
			? ""
			: params.getIwsBlockTmScoreValue());
		chkIwsBlockMultipleExact.setSelection(params.isIwsBlockMultipleExact());
		chkIwsIncludeMultipleExact.setSelection(params.isIwsIncludeMultipleExact());

		edSubfilterId.setText(params.getCdataSubfilter() == null ? "" : params.getCdataSubfilter());

		chkUseCodeFinder.setSelection(params.getUseCodeFinder());

		pnlCodeFinder.setRules(params.getCodeFinderData());

		updateInlineCodes();
		pnlCodeFinder.updateDisplay();
	}

	private boolean saveData() {
		params.setUseCodeFinder(chkUseCodeFinder.getSelection());
		if (chkUseCodeFinder.getSelection()) {
			if (pnlCodeFinder.getRules() == null) {
				return false;
			} else {
				params.setCodeFinderData(pnlCodeFinder.getRules());
			}
		}

		params.setFallbackToID(chkFallBackToId.getSelection());
		params.setIgnoreInputSegmentation(
				chkIgnoreInputSegmentation.getSelection());
		params.setAlwaysUseSegSource(chkAlwaysUseSegSource.getSelection());
		params.setUseSdlXliffWriter(chkUseSdlXliffWriter.getSelection());
		params.setEscapeGT(chkEscapeGt.getSelection());
		params.setAddTargetLanguage(chkAddTargetLanguage.getSelection());
		params.setOverrideTargetLanguage(
				chkOverrideTargetLanguage.getSelection());
		params.setAllowEmptyTargets(chkAllowEmptyTargets.getSelection());
		String[] segTypes = listOutputSegmentationType.getSelection();
		if (segTypes.length > 0) {
			getSegTypeForLabel(segTypes[0])
					.ifPresent(type -> params.setOutputSegmentationType(type));
		}
		params.setAddAltTrans(chkAddAltTrans.getSelection());
		params.setIncludeIts(chkIncludeITS.getSelection());
		params.setBalanceCodes(chkBalanceCodes.getSelection());
		params.setPreserveSpaceByDefault(
				chkPreserveSpaceByDefault.getSelection());
		params.setInlineCdata(chkInlineCdata.getSelection());
		params.setSkipNoMrkSegSource(chkSkipNoMrkSegSource.getSelection());
		params.setUseCustomParser(chkUseCustomParser.getSelection());
		params.setFactoryClass(edCustomParserClass.getText());
		params.setCdataSubfilter(edSubfilterId.getText());

		params.setUseIwsXliffWriter(chkUseIwsXliffWriter.getSelection());
		params.setIwsBlockFinished(chkIwsBlockFinished.getSelection());
		params.setIwsTransStatusValue(edTransStatusValue.getText());
		params.setIwsRemoveTmOrigin(chkIwsRemoveTmOrigin.getSelection());
		params.setIwsTransTypeValue(edTransTypeValue.getText());
		params.setIwsBlockLockStatus(chkIwsBlockLockStatus.getSelection());
		params.setIwsBlockTmScore(chkIwsBlockTmScore.getSelection());
		params.setIwsBlockTmScoreValue(edIwsBlockTmScoreValue.getText());
		params.setIwsBlockMultipleExact(chkIwsBlockMultipleExact.getSelection());
		params.setIwsIncludeMultipleExact(chkIwsIncludeMultipleExact.getSelection());

		return true;
	}

	private void updateInlineCodes() {
		pnlCodeFinder.setEnabled(chkUseCodeFinder.getSelection());
	}

}