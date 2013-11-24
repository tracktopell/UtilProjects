/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tracktopell.dao.builder.xpressosystems.framework.control;

import com.tracktopell.dao.builder.metadata.Column;
import com.tracktopell.dao.builder.metadata.DBTableSet;
import com.tracktopell.dao.builder.metadata.Table;
import com.tracktopell.dao.builder.parser.VP6Parser;
import com.tracktopell.dao.builder.parser.VP6ParserForXpressoSystems;
import com.tracktopell.dao.builder.parser.VPModel;
import com.tracktopell.dao.builder.xpressosystems.framework.view.WizzardPanel;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author alfredo
 */
public class WizzardControl implements ActionListener, WindowListener, ListSelectionListener {

	private WizzardPanel wizzardPanel;
	private JFrame wizzardFrame;
	private DBTableSet dbSet = null;
	private List<Table> tablesSortedForCreation = null;
	private Table selectedTable = null;
	private Column selectedColumn = null;

	WizzardControl() {
		wizzardPanel = new WizzardPanel();

		wizzardPanel.getBackBtn().addActionListener(this);
		wizzardPanel.getNextBtn().addActionListener(this);
		wizzardPanel.getFinishBtn().addActionListener(this);


		wizzardFrame = new JFrame("VPModel 2 XpressoSystems Metadata Mapping >> FastWizzard !");
		wizzardFrame.setSize(800, 400);
		wizzardFrame.addWindowListener(this);
		wizzardFrame.getContentPane().add(wizzardPanel);
		//wizzardFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		wizzardPanel.getTablesPropertiesTbl().getSelectionModel().addListSelectionListener(this);
		wizzardPanel.getColumnsPropertiesTbl().getSelectionModel().addListSelectionListener(this);

		wizzardPanel.getParseBtn().addActionListener(this);
	}

	@Override
	public void valueChanged(ListSelectionEvent lse) {
		if (lse.getSource() == wizzardPanel.getTablesPropertiesTbl().getSelectionModel()) {
			System.err.println("-->>TablesPropertiesTbl.selecting(" + lse.getFirstIndex() + "," + lse.getLastIndex() + "," + lse.getValueIsAdjusting() + ")");
			if (!lse.getValueIsAdjusting() && lse.getFirstIndex() >= 0 && lse.getFirstIndex() <= lse.getLastIndex()) {
				updateSelectedTableInfo(lse.getFirstIndex());
			}
		} else if (lse.getSource() == wizzardPanel.getColumnsPropertiesTbl().getSelectionModel()) {
			System.err.println("-->>ColumnsPropertiesTbl.selecting(" + lse.getFirstIndex() + "," + lse.getLastIndex() + "," + lse.getValueIsAdjusting() + ")");
			if (!lse.getValueIsAdjusting() && lse.getFirstIndex() >= 0 && lse.getFirstIndex() <= lse.getLastIndex()) {
				updateSelectedColumnInfo(lse.getFirstIndex());
			}
		}
	}

	private void updateSelectedTableInfo(int tableIndex) {
		selectedTable = tablesSortedForCreation.get(tableIndex);

		Hashtable<String, String> metaProperties = selectedTable.getMetaProperties();
		Set<String> keySet = metaProperties.keySet();
		Object[] columnMetaNames = {"META", "Valor"};
		Object[][] dataMeta = new Object[keySet.size()][];
		int i = 0;
		for (String k : keySet) {
			dataMeta[i] = new Object[2];
			dataMeta[i][0] = k;
			dataMeta[i][1] = metaProperties.get(k);
			i++;
		}

		wizzardPanel.getTablesMetaPropertiesTbl().setModel(new DefaultTableModel(dataMeta, columnMetaNames));

		//----------------------------------------------------------------------

		Object[] columnNames = {"Orden", "Nombre", "Tipo SQL", "Tipo JDBC", "PK", "FK", "i++", "NULL"};
		Collection<Column> colums = selectedTable.getColums();

		Object[][] data = new Object[colums.size()][];
		int j = 0;
		for (Column c : colums) {
			data[j] = new Object[8];
			data[j][0] = c.getPosition();
			data[j][1] = c.getName();
			data[j][2] = c.getSqlType();
			data[j][3] = c.getJavaClassType();
			data[j][4] = c.isPrimaryKey();
			data[j][5] = c.isForeignKey();
			data[j][6] = c.isAutoIncremment();
			data[j][7] = c.isNullable();
			j++;
		}

		wizzardPanel.getColumnsPropertiesTbl().setModel(new DefaultTableModel(data, columnNames));


	}

	private void updateSelectedColumnInfo(int columnIndex) {
		selectedColumn = (Column) selectedTable.getColums().toArray()[columnIndex];

		Hashtable<String, String> metaProperties = selectedColumn.getMetaProperties();
		Set<String> keySet = metaProperties.keySet();
		Object[] columnMetaNames = {"META", "Valor"};
		Object[][] dataMeta = new Object[keySet.size()][];
		int i = 0;
		for (String k : keySet) {
			dataMeta[i] = new Object[2];
			dataMeta[i][0] = k;
			dataMeta[i][1] = metaProperties.get(k);
			i++;
		}
		wizzardPanel.getColumnsMetaPropertiesTbl().setModel(new DefaultTableModel(dataMeta, columnMetaNames));

	}

	public void estadoInicial() {
		wizzardPanel.getBackBtn().setEnabled(false);
		//wizzardPanel.getNextBtn().setEnabled(false);
		//wizzardPanel.getFinishBtn().setEnabled(false);

		wizzardFrame.setVisible(true);
		wizzardIndex = 0;
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		if (ae.getSource() == wizzardPanel.getBackBtn()) {
			prevSteep();
		} else if (ae.getSource() == wizzardPanel.getNextBtn()) {
			nextSteep();
		} else if (ae.getSource() == wizzardPanel.getFinishBtn()) {
			finishSteep();
		} else if (ae.getSource() == wizzardPanel.getParseBtn()) {
			parseVPProject();
		}
	}
	private int wizzardIndex = 0;
	private String[] cardsNames = {"importAndChoicePanel", "tablesAndColumnsCustPanel", "generatedOutputPanel", "resultsPanel"};

	private void selccionaTablas() {

		int[] selectedRows = wizzardPanel.getVpTablesToChoice().getSelectedRows();
		if (selectedRows == null || selectedRows.length == 0) {
			JOptionPane.showMessageDialog(wizzardFrame, "DEbe seleccionar las talbas", "Scan", JOptionPane.WARNING_MESSAGE);
			return;
		}

		String[] tableNames2Gen = new String[selectedRows.length];
		for (int i = 0; i < selectedRows.length; i++) {
			tableNames2Gen[i] = tablesSortedForCreation.get(i).getName();
		}


		dbSet = dbSet.copyJustSelectedNames(tableNames2Gen);
		tablesSortedForCreation = dbSet.getTablesSortedForCreation();

		Object[] columnNames = {"Tabla", "Entidad"};
		Object[][] data = new Object[tablesSortedForCreation.size()][];
		int i = 0;
		for (Table t : tablesSortedForCreation) {
			data[i] = new Object[2];
			data[i][0] = t.getName();
			data[i][1] = t.getJavaDeclaredName();
			i++;
		}

		wizzardPanel.getTablesPropertiesTbl().setModel(new DefaultTableModel(data, columnNames));
		selectNextPanel();
	}

	private void selectNextPanel() {
		CardLayout clWizzard = (CardLayout) wizzardPanel.getCardPanel().getLayout();
		wizzardIndex++;
		clWizzard.show(wizzardPanel.getCardPanel(), cardsNames[wizzardIndex]);

	}

	private void nextSteep() {

		if (wizzardIndex < cardsNames.length - 1) {
			if (wizzardIndex == 0) {
				selccionaTablas();
			}
			/*
			 wizzardIndex++;
			 clWizzard.show(wizzardPanel.getCardPanel(), cardsNames[wizzardIndex]);
			 if (wizzardIndex == cardsNames.length - 1) {
			 wizzardPanel.getBackBtn().setEnabled(true);
			 wizzardPanel.getNextBtn().setEnabled(false);
			 wizzardPanel.getFinishBtn().setEnabled(false);
			 } else {
			 wizzardPanel.getBackBtn().setEnabled(true);
			 wizzardPanel.getNextBtn().setEnabled(true);
			 wizzardPanel.getFinishBtn().setEnabled(true);
			 }
			 */
		} else if (wizzardIndex == cardsNames.length - 1) {
			throw new IllegalStateException("> X");
		}
	}

	private void prevSteep() {
		CardLayout clWizzard = (CardLayout) wizzardPanel.getCardPanel().getLayout();
		if (wizzardIndex > 0) {
			wizzardIndex--;
			clWizzard.show(wizzardPanel.getCardPanel(), cardsNames[wizzardIndex]);
			if (wizzardIndex == 0) {
				wizzardPanel.getBackBtn().setEnabled(false);
				wizzardPanel.getNextBtn().setEnabled(true);
				wizzardPanel.getFinishBtn().setEnabled(true);
			} else {
				wizzardPanel.getBackBtn().setEnabled(true);
				wizzardPanel.getNextBtn().setEnabled(true);
				wizzardPanel.getFinishBtn().setEnabled(true);
			}
		} else if (wizzardIndex == cardsNames.length - 1) {
			throw new IllegalStateException("< X");
		}
	}

	private void finishSteep() {
		CardLayout clWizzard = (CardLayout) wizzardPanel.getCardPanel().getLayout();
		if (wizzardIndex < cardsNames.length - 1) {
			wizzardIndex = cardsNames.length - 1;
			clWizzard.show(wizzardPanel.getCardPanel(), cardsNames[wizzardIndex]);

			wizzardPanel.getBackBtn().setEnabled(true);
			wizzardPanel.getNextBtn().setEnabled(false);
			wizzardPanel.getFinishBtn().setEnabled(false);

		} else if (wizzardIndex == cardsNames.length - 1) {
			throw new IllegalStateException(">>| X");
		}
	}

	@Override
	public void windowOpened(WindowEvent we) {
	}

	@Override
	public void windowClosing(WindowEvent we) {
		int r = JOptionPane.showConfirmDialog(wizzardFrame, "¿Esta seguroi de salir ?",
				"Salir", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		System.out.println("==>> ?" + r);
		if (r == JOptionPane.OK_OPTION) {
			wizzardFrame.dispose();
			System.out.println("\t==>> Dispose !");
			System.exit(0);
		}
	}

	@Override
	public void windowClosed(WindowEvent we) {
	}

	@Override
	public void windowIconified(WindowEvent we) {
	}

	@Override
	public void windowDeiconified(WindowEvent we) {
	}

	@Override
	public void windowActivated(WindowEvent we) {
	}

	@Override
	public void windowDeactivated(WindowEvent we) {
	}

	private void parseVPProject() {
		String pathToVPProject = null;

		Hashtable<String, VPModel> vpModels;
		try {
			pathToVPProject = wizzardPanel.getXmlVPFilePath().getText().trim();

			vpModels = VP6Parser.loadVPModels(new FileInputStream(pathToVPProject));

			dbSet = VP6ParserForXpressoSystems.loadFromXMLWithVPModels(new FileInputStream(pathToVPProject), vpModels);
			tablesSortedForCreation = dbSet.getTablesSortedForCreation();
			Object[] columnNames = {"Tabla", "Entidad"};
			Object[][] data = new Object[tablesSortedForCreation.size()][];
			int i = 0;
			for (Table t : tablesSortedForCreation) {
				data[i] = new Object[2];
				data[i][0] = t.getName();
				data[i][1] = t.getJavaDeclaredName();
				i++;
			}

			wizzardPanel.getVpTablesToChoice().setModel(new DefaultTableModel(data, columnNames));

		} catch (FileNotFoundException ex) {

			JOptionPane.showMessageDialog(wizzardFrame, ex.getLocalizedMessage(), "Import & parse", JOptionPane.ERROR_MESSAGE);
			return;
		}
	}
}
