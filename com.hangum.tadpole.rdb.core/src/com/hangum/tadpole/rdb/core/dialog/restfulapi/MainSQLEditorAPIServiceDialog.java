/*******************************************************************************
 * Copyright (c) 2012 - 2015 hangum.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     hangum - initial API and implementation
 ******************************************************************************/
package com.hangum.tadpole.rdb.core.dialog.restfulapi;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.google.gson.JsonArray;
import com.hangum.tadpole.commons.dialogs.message.dao.RequestResultDAO;
import com.hangum.tadpole.commons.google.analytics.AnalyticCaller;
import com.hangum.tadpole.commons.libs.core.define.PublicTadpoleDefine;
import com.hangum.tadpole.commons.libs.core.message.CommonMessages;
import com.hangum.tadpole.commons.libs.core.utils.VelocityUtils;
import com.hangum.tadpole.commons.util.GlobalImageUtils;
import com.hangum.tadpole.commons.util.JSONUtil;
import com.hangum.tadpole.engine.query.dao.system.UserDBDAO;
import com.hangum.tadpole.engine.query.sql.TadpoleSystem_ExecutedSQL;
import com.hangum.tadpole.engine.restful.RESTfulAPIUtils;
import com.hangum.tadpole.engine.sql.paremeter.NamedParameterDAO;
import com.hangum.tadpole.engine.sql.paremeter.NamedParameterUtil;
import com.hangum.tadpole.engine.sql.util.ExecuteDMLCommand;
import com.hangum.tadpole.engine.sql.util.SQLUtil;
import com.hangum.tadpole.engine.utils.RequestQuery;
import com.hangum.tadpole.engine.utils.RequestQueryUtil;
import com.hangum.tadpole.rdb.core.Messages;
import com.hangum.tadpole.session.manager.SessionManager;

/**
 * Test API service dialog
 *
 * @author hangum
 * @version 1.6.1
 * @since 2015. 5. 19.
 *
 */
public class MainSQLEditorAPIServiceDialog extends Dialog {
	private static final Logger logger = Logger.getLogger(MainSQLEditorAPIServiceDialog.class);
	private UserDBDAO userDB;
	private String strOriginalSQL;
	
	private Combo comboResultType;
	private Text textArgument;
	
	private Text textOriginal;
	private Text textResultSQL;
	private Text textResult;
	
	private Button btnAddHeader;
	private Text textDelimiter;
	
	/**
	 * Create the dialog.
	 * @param parentShell
	 * @param strOriginalSQL
	 */
	public MainSQLEditorAPIServiceDialog(Shell parentShell, UserDBDAO userDB, String strOriginalSQL) {
		super(parentShell);
		setShellStyle(SWT.MAX | SWT.RESIZE | SWT.TITLE | SWT.APPLICATION_MODAL);
		this.userDB = userDB;
		this.strOriginalSQL = strOriginalSQL;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.get().MainSQLEditorAPIServiceDialog_0);
		newShell.setImage(GlobalImageUtils.getTadpoleIcon());
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = (GridLayout) container.getLayout();
		gridLayout.marginHeight = 5;
		gridLayout.marginWidth = 5;
		
		Composite compositeTitle = new Composite(container, SWT.NONE);
		compositeTitle.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		compositeTitle.setLayout(new GridLayout(2, false));
		
		Label lblArgument = new Label(compositeTitle, SWT.NONE);
		lblArgument.setText(Messages.get().Argument);
		
		textArgument = new Text(compositeTitle, SWT.BORDER);
		textArgument.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				makeParameter();
				executeQuery(textOriginal.getText(), textArgument.getText());
				
				textArgument.setFocus();
			}
		});
		textArgument.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Label lblType = new Label(compositeTitle, SWT.NONE);
		lblType.setText(Messages.get().Result);
		
		comboResultType = new Combo(compositeTitle, SWT.READ_ONLY);
		comboResultType.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				initResultType();
			}
		});
		comboResultType.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		for (ExecuteDMLCommand.RESULT_TYPE resultType : ExecuteDMLCommand.RESULT_TYPE.values()) {
			comboResultType.add(resultType.name());
		}
		comboResultType.select(0);
		
		Composite compositeDetailCSV = new Composite(compositeTitle, SWT.NONE);
		compositeDetailCSV.setLayout(new GridLayout(3, false));
		compositeDetailCSV.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		btnAddHeader = new Button(compositeDetailCSV, SWT.CHECK);
		btnAddHeader.setText(Messages.get().Header);
		
		Label lblDelimiter = new Label(compositeDetailCSV, SWT.NONE);
		lblDelimiter.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblDelimiter.setText(Messages.get().Delimiter);
		
		textDelimiter = new Text(compositeDetailCSV, SWT.BORDER);
		textDelimiter.setEditable(false);
		textDelimiter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		SashForm sashFormMain = new SashForm(container, SWT.VERTICAL);
		sashFormMain.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		SashForm sashFormVariable = new SashForm(sashFormMain, SWT.NONE);
		sashFormVariable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		Group grpOriginalSql = new Group(sashFormVariable, SWT.NONE);
		grpOriginalSql.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		grpOriginalSql.setText(Messages.get().MainSQLEditorAPIServiceDialog_2);
		grpOriginalSql.setLayout(new GridLayout(1, false));
		
		textOriginal = new Text(grpOriginalSql, SWT.BORDER | SWT.MULTI);
		textOriginal.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		Group grpResultSql = new Group(sashFormVariable, SWT.NONE);
		grpResultSql.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		grpResultSql.setText(Messages.get().MainSQLEditorAPIServiceDialog_3);
		grpResultSql.setLayout(new GridLayout(1, false));
		
		textResultSQL = new Text(grpResultSql, SWT.BORDER | SWT.MULTI);
		textResultSQL.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		
		Group grpResult = new Group(sashFormMain, SWT.NONE);
		grpResult.setLayout(new GridLayout(1, false));
		grpResult.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		grpResult.setText(Messages.get().Result);
		
		textResult = new Text(grpResult, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.CANCEL | SWT.MULTI);
		textResult.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		sashFormMain.setWeights(new int[] {7, 3});
		initUI();

		AnalyticCaller.track(this.getClass().getName());
		
		return container;
	}
	
	/**
	 * initialize result type
	 */
	private void initResultType() {
		boolean isEnable = false;
		if(ExecuteDMLCommand.RESULT_TYPE.CSV.name().equals(comboResultType.getText())) {
			isEnable = true;
		}
		
		btnAddHeader.setEnabled(isEnable);
		textDelimiter.setEditable(isEnable);
	}
	
	/**
	 * initialize UI
	 */
	private void initUI() {
		initResultType();
		textOriginal.setText(strOriginalSQL);
		textArgument.setText(RESTfulAPIUtils.getParameter(strOriginalSQL));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	@Override
	protected void okPressed() {
		makeParameter();
		executeQuery(textOriginal.getText(), textArgument.getText());
		
		textArgument.setFocus();
	}
	
	/**
	 * make parameter
	 */
	private void makeParameter() {
		try {
			Map<String, Object> mapParameter = RESTfulAPIUtils.maekArgumentTOMap(textArgument.getText());
			String strResult = VelocityUtils.getTemplate("MainEditorTest", textOriginal.getText(), mapParameter); //$NON-NLS-1$
			if(logger.isDebugEnabled()) logger.debug(strResult);
			textResultSQL.setText(strResult);
			
		} catch(Exception e) {
			logger.error("Template Exception", e); //$NON-NLS-1$
			MessageDialog.openError(getShell(),CommonMessages.get().Error, Messages.get().MainSQLEditorAPIServiceDialog_7);
			
			return;
		} 
	}
	
	/**
	 * 쿼리 수행
	 * 
	 * @param strSQL
	 * @param strArgument
	 */
	private void executeQuery(String strSQL, String strArgument) {
		executeQuery(RequestQueryUtil.simpleRequestQuery(userDB, strSQL), strArgument);
	}
	
	/**
	 * 검색
	 * 
	 * @param strSQL
	 * @param strArgument
	 */
	private void executeQuery(RequestQuery reqQuery, String strArgument) {
		String strExecuteResultData = ""; //$NON-NLS-1$
		String strSQLs = "";
		
		RequestResultDAO reqResultDAO = new RequestResultDAO();
		reqResultDAO.setEXECUSTE_SQL_TYPE(PublicTadpoleDefine.EXECUTE_SQL_TYPE.API_USER);
		reqResultDAO.setDbSeq(userDB.getSeq());
		reqResultDAO.setStartDateExecute(new Timestamp(System.currentTimeMillis()));
		reqResultDAO.setIpAddress(SessionManager.getLoginIp());
		
		try {
			int intSQLCnt = 0;
			// velocity 로 if else 가 있는지 검사합니다. 
			strSQLs = RESTfulAPIUtils.makeTemplateTOSQL("APIServiceDialog", reqQuery.getSql(), strArgument); //$NON-NLS-1$
			// 분리자 만큼 실행한다.
			for (String strTmpSQL : strSQLs.split(PublicTadpoleDefine.SQL_DELIMITER)) {
				String strRealSQL = SQLUtil.removeCommentAndOthers(userDB, strTmpSQL);
				if(StringUtils.trim(strRealSQL).equals("")) continue;
				intSQLCnt++;
				
				NamedParameterDAO dao = NamedParameterUtil.parseParameterUtils(userDB, strTmpSQL, strArgument);
				if(ExecuteDMLCommand.RESULT_TYPE.JSON.name().equalsIgnoreCase(comboResultType.getText())) {
					strExecuteResultData += getSelect(userDB, dao.getStrSQL(), dao.getListParam()) + ","; //$NON-NLS-1$
				} else {
					strExecuteResultData += getSelect(userDB, dao.getStrSQL(), dao.getListParam());
				}
			}
			
			if(intSQLCnt > 1) {	
				if(ExecuteDMLCommand.RESULT_TYPE.JSON.name().equalsIgnoreCase(comboResultType.getText())) {
					strExecuteResultData = "[" + StringUtils.removeEnd(strExecuteResultData, ",") + "]";  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			}
			
			textResult.setText(strExecuteResultData);
			
			reqResultDAO.setResult(PublicTadpoleDefine.SUCCESS_FAIL.S.toString());
		} catch (Exception e) {
			logger.error("api exception", e); //$NON-NLS-1$
			
			MessageDialog.openError(getShell(),CommonMessages.get().Error, e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			reqResultDAO.setEndDateExecute(new Timestamp(System.currentTimeMillis()));
			reqResultDAO.setTdb_sql_head("/** api key : API Hub Manager */\r\n/**" + strArgument + "*/");
			reqResultDAO.setSql_text(strSQLs);
			reqResultDAO.setResultData(strExecuteResultData);
			
			try {
				TadpoleSystem_ExecutedSQL.saveExecuteSQUeryResource(SessionManager.getUserSeq(), 
						userDB, 
						reqResultDAO
					);
			} catch(Exception e) {
				logger.error("save history", e);
			}
		}
	}
	
	/**
	 * called sql
	 * 
	 * @param userDB
	 * @param strSQL
	 * @param listParam
	 * @return
	 * @throws Exception
	 */
	private String getSelect(final UserDBDAO userDB, String strSQL, List<Object> listParam) throws Exception {
		String strResult = ""; //$NON-NLS-1$
		
		if(SQLUtil.isStatement(strSQL)) {
			
			if(ExecuteDMLCommand.RESULT_TYPE.JSON.name().equals(comboResultType.getText())) {
				JsonArray jsonArry = ExecuteDMLCommand.selectToJson(userDB, strSQL, listParam);
				strResult = JSONUtil.getPretty(jsonArry.toString());
			} else if(ExecuteDMLCommand.RESULT_TYPE.CSV.name().equals(comboResultType.getText())) {
				strResult = ExecuteDMLCommand.selectToCSV(userDB, strSQL, listParam, btnAddHeader.getSelection(), textDelimiter.getText());
			} else if(ExecuteDMLCommand.RESULT_TYPE.XML.name().equals(comboResultType.getText())) {
				strResult = ExecuteDMLCommand.selectToXML(userDB, strSQL, listParam);
			} else {
				strResult = ExecuteDMLCommand.selectToHTML_TABLE(userDB, strSQL, listParam);
			}
		} else {
			strResult = ExecuteDMLCommand.executeDML(userDB, strSQL, listParam, comboResultType.getText());
		}
		
		return strResult;
	}

	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, CommonMessages.get().Run, true);
		createButton(parent, IDialogConstants.CANCEL_ID, CommonMessages.get().Close, false);
	}
	
	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(650, 650);
	}
}
