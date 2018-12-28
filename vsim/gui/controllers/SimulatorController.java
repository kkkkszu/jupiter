package vsim.gui.controllers;

import java.io.File;
import vsim.Globals;
import vsim.Settings;
import vsim.utils.Cmd;
import javafx.fxml.FXML;
import vsim.linker.Linker;
import vsim.utils.Message;
import java.io.FileWriter;
import java.io.IOException;
import vsim.riscv.Register;
import java.util.ArrayList;
import vsim.gui.utils.Icons;
import javafx.util.Callback;
import vsim.simulator.Status;
import vsim.riscv.MemoryCell;
import java.io.BufferedWriter;
import javafx.css.PseudoClass;
import javafx.concurrent.Task;
import vsim.simulator.Debugger;
import javafx.scene.control.Tab;
import vsim.assembler.Assembler;
import javafx.stage.FileChooser;
import vsim.linker.LinkedProgram;
import vsim.linker.InfoStatement;
import vsim.riscv.MemorySegments;
import javafx.application.Platform;
import vsim.gui.components.InfoCell;
import javafx.beans.binding.Bindings;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableCell;
import com.jfoenix.controls.JFXButton;
import javafx.scene.control.TableView;
import com.jfoenix.controls.JFXTabPane;
import vsim.gui.components.BooleanCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ContextMenu;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener;
import vsim.gui.components.MemoryEditingCell;
import javafx.scene.control.SeparatorMenuItem;
import vsim.gui.components.RegisterEditingCell;
import javafx.collections.ListChangeListener.Change;
import com.sun.javafx.scene.control.skin.VirtualFlow;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.cell.PropertyValueFactory;
import com.sun.javafx.scene.control.skin.TableViewSkin;
import static vsim.riscv.instructions.Instruction.LENGTH;


/**
 * Simulator controller class.
 */
public class SimulatorController {

  /** Simulator go button */
  @FXML protected JFXButton goBtn;
  /** Simulator stop button */
  @FXML protected JFXButton stopBtn;
  /** Simulator step button */
  @FXML protected JFXButton stepBtn;
  /** Simulator backstep button */
  @FXML protected JFXButton backstepBtn;
  /** Simulator reset button */
  @FXML protected JFXButton resetBtn;
  /** Simulator dump button */
  @FXML protected JFXButton dumpBtn;

  /** Simulator text segment table view */
  @FXML protected TableView<InfoStatement> textTable;
  /** Simulator text segment breakpoint column */
  @FXML protected TableColumn<InfoStatement, Boolean> txtBkptCol;
  /** Simulator text segment table view memory address column */
  @FXML protected TableColumn<InfoStatement, String> txtAddrCol;
  /** Simulator text segment table view machine code column */
  @FXML protected TableColumn<InfoStatement, String> txtMachineCode;
  /** Simulator text segment table view source code column */
  @FXML protected TableColumn<InfoStatement, String> txtSourceCode;
  /** Simulator text segment table view basic code column */
  @FXML protected TableColumn<InfoStatement, String> txtBasicCode;

  /** Hardware tab pane */
  @FXML protected JFXTabPane hardware;

  /** RISC-V RVI register file tab */
  @FXML protected Tab rviTab;
  /** RISC-V RVI register file table view */
  @FXML protected TableView<Register> rviTable;
  /** RISC-V RVI register file table view register mnemonic column */
  @FXML protected TableColumn<Register, String> rviMnemonic;
  /** RISC-V RVI register file table view register number column */
  @FXML protected TableColumn<Register, Integer> rviNumber;
  /** RISC-V RVI register file table view register value column */
  @FXML protected TableColumn<Register, String> rviValue;

  /** RISC-V RVF register file tab */
  @FXML protected Tab rvfTab;
  /** RISC-V RVF register file table view */
  @FXML protected TableView<Register> rvfTable;
  /** RISC-V RVF register file table view register mnemonic column */
  @FXML protected TableColumn<Register, String> rvfMnemonic;
  /** RISC-V RVF register file table view register number column */
  @FXML protected TableColumn<Register, Integer> rvfNumber;
  /** RISC-V RVF register file table view register value column */
  @FXML protected TableColumn<Register, String> rvfValue;

  /** RISC-V memory tab */
  @FXML protected Tab memTab;
  /** RISC-V memory table view */
  @FXML protected TableView<MemoryCell> memTable;
  /** RISC-V memory table view address column */
  @FXML protected TableColumn<MemoryCell, String> memAddress;
  /** RISC-V memory table view offset 0 column */
  @FXML protected TableColumn<MemoryCell, String> memOffset0;
  /** RISC-V memory table view offset 1 column */
  @FXML protected TableColumn<MemoryCell, String> memOffset1;
  /** RISC-V memory table view offset 2 column */
  @FXML protected TableColumn<MemoryCell, String> memOffset2;
  /** RISC-V memory table view offset 3 column */
  @FXML protected TableColumn<MemoryCell, String> memOffset3;

  /** RISC-V memory up button */
  @FXML protected JFXButton upBtn;
  /** RISC-V memory down button */
  @FXML protected JFXButton downBtn;

  /** Reference to main controller */
  private MainController mainController;

  /** V-Sim debugger */
  private Debugger debugger;

  /** Text table virtual flow */
  private VirtualFlow<?> vflow;
  /** RVI table virtual flow */
  private VirtualFlow<?> rviVFlow;
  /** RVF table virtual flow */
  private VirtualFlow<?> rvfVFlow;

  /** Last register modified */
  private Register lastReg;

  /** current go task */
  private Task<Boolean> goTask;

  /**
   * Initialize simulator controller.
   *
   * @param controller main controller
   */
  protected void initialize(MainController controller) {
    this.mainController = controller;
    this.initRegFiles();
    this.initMemory();
    this.initText();
    this.initButtons();
  }

  /*-------------------------------------------------------*
   |                  protected actions                    |
   *-------------------------------------------------------*/

  /**
   * Assembles all files in directory.
   */
  protected void assemble() {
    // reset simulator state
    Status.reset();
    Globals.reset();
    this.debugger = null;
    this.textTable.getItems().clear();
    Status.READY.set(false);
    ArrayList<File> files = new ArrayList<File>();
    if (Cmd.getFilesInDir(files)) {
      LinkedProgram program = Linker.link(Assembler.assemble(files));
      if (program != null) {
        program.reset();
        this.debugger = new Debugger(program);
        Status.READY.set(true);
        ObservableList<InfoStatement> stmts = program.getInfoStatements();
        for (InfoStatement stmt: stmts)
          stmt.breakpointProperty().addListener((e, oldVal, newVal) -> this.breakpoint(newVal, stmt));
        this.textTable.setItems(stmts);
        this.mainController.selectSimulatorTab();
        /*
          Align table columns

          Ref:
          https://stackoverflow.com/questions/37423748/javafx-tablecolumns-headers-not-aligned-with-cells-due-to-vertical-scrollbar
        */
        Platform.runLater(() -> {
          this.textTable.refresh();
          this.vflow = (VirtualFlow<?>)((TableViewSkin<?>)this.textTable.getSkin()).getChildren().get(1);
        });
      }
    };
  }

  /**
   * Go simulator control, runs all the program.
   */
  protected void go() {
    Status.STOPPED.set(false);
    this.goTask = new Task<Boolean>() {
      @Override
      protected Boolean call() throws Exception {
        Status.RUNNING.set(true);
        refreshTables();
        while (!this.isCancelled() && debugger.step(true));
        Status.RUNNING.set(false);
        refreshTables();
        if (this.isCancelled())
          return false;
        return true;
      }
    };
    Thread th = new Thread(this.goTask);
    th.setDaemon(true);
    th.start();
  }

  /**
   * Stop simulator control, stops program execution.
   */
  protected void stop() {
    Status.STOPPED.set(true);
    this.goTask.cancel();
  }

  /**
   * Step flow control, advances the simulator by 1 step.
   */
  protected void step() {
    Status.STOPPED.set(false);
    Thread th = new Thread(new Task<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        debugger.step(false);
        return true;
      }
    });
    th.setDaemon(true);
    th.start();
  }

  /**
   * Backstep flow control, goes back to the previous step.
   */
  protected void backstep() {
    this.debugger.backstep();
  }

  /**
   * Resets all the simulator state and starts again.
   */
  protected void reset() {
    this.debugger.reset();
  }

  /**
   * Clear all breakpoints that were set.
   */
  protected void clearAllBreakpoints() {
    for (InfoStatement stmt: this.textTable.getItems()) {
      if (!stmt.isEbreak() && stmt.getBreakpoint())
        stmt.setBreakpoint(false);
    }
  }

  /**
   * Dumps generated machine code to a file.
   */
  protected void dump() {
    FileChooser chooser = new FileChooser();
    chooser.setTitle("Dump Machine Code To File");
    File file = chooser.showSaveDialog(this.mainController.stage);
    if (file != null) {
      try {
        if (!file.exists())
          file.createNewFile();
      } catch (IOException e) {
        Message.error("the file " + file + " could not be created");
        return;
      }
      try {
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        for (InfoStatement stmt: this.textTable.getItems()) {
          bw.write(stmt.getMachineCode());
          bw.newLine();
        }
        bw.close();
        Message.log("machine code dumped to: " + file);
      } catch (IOException e) {
        Message.error("the file " + file  + " could not be written");
        if (file.exists() && file.length() == 0)
          file.delete();
      }
    }
  }

  /*-------------------------------------------------------*
   |                   private actions                     |
   *-------------------------------------------------------*/

  /**
   * This methods adds or deletes a breakpoint at the stament address
   *
   * @param add true if breakpoint will be added, false if will be deleted
   * @param stmt program info statement
   */
  private void breakpoint(boolean add, InfoStatement stmt) {
    String address = stmt.getAddress();
    if (add)
      this.debugger.breakpoint(address);
    else
      this.debugger.delete(address);
    Platform.runLater(() -> this.textTable.refresh());
  }

  /**
   * Refreshes all simulator tables views.
   */
  private void refreshTables() {
    Platform.runLater(() -> {
      this.textTable.refresh();
      this.rviTable.refresh();
      this.rvfTable.refresh();
      this.memTable.refresh();
    });
  }

  /**
   * Changes memory display setting to hexadecimal.
   */
  private void memoryDisplayHex() {
    Settings.DISP_MEM_CELL = 0;
    Platform.runLater(() -> {
      for (MemoryCell cell: this.memTable.getItems())
        cell.update();
    });
  }

  /**
   * Changes memory display setting to ascii.
   */
  private void memoryDisplayAscii() {
    Settings.DISP_MEM_CELL = 1;
    Platform.runLater(() -> {
      for (MemoryCell cell: this.memTable.getItems())
        cell.update();
    });
  }

  /**
   * Changes memory display setting to decimal.
   */
  private void memoryDisplayDecimal() {
    Settings.DISP_MEM_CELL = 2;
    Platform.runLater(() -> {
      for (MemoryCell cell: this.memTable.getItems())
        cell.update();
    });
  }

  /**
   * Changes rvi register display setting to decimal.
   */
  private void rviDisplayDecimal() {
    Settings.DISP_RVI_REG = 2;
    Platform.runLater(() -> {
      for (Register reg: this.rviTable.getItems())
        reg.update();
    });
  }

  /**
   * Changes rvi register display setting to unsigned.
   */
  private void rviDisplayUnsigned() {
    Settings.DISP_RVI_REG = 1;
    Platform.runLater(() -> {
      for (Register reg: this.rviTable.getItems())
        reg.update();
    });
  }

  /**
   * Changes rvi register display setting to hexadecimal.
   */
  private void rviDisplayHex() {
    Settings.DISP_RVI_REG = 0;
    Platform.runLater(() -> {
      for (Register reg: this.rviTable.getItems())
        reg.update();
    });
  }

  /**
   * Changes rvf register display setting to hexadecimal.
   */
  private void rvfDisplayHex() {
    Settings.DISP_RVF_REG = 0;
    Platform.runLater(() -> {
      for (Register reg: this.rvfTable.getItems())
        reg.update();
    });
  }

  /**
   * Changes rvf register display setting to float.
   */
  private void rvfDisplayFloat() {
    Settings.DISP_RVF_REG = 1;
    Platform.runLater(() -> {
      for (Register reg: this.rvfTable.getItems())
        reg.update();
    });
  }

  /**
   * Updates an editable memory cell offset column.
   *
   * @param t cell edit event
   */
  private void updateMemoryCell(CellEditEvent<MemoryCell, String> t) {
    // get offset
    int offset = t.getTablePosition().getColumn() - 1;
    // get memory cell to get integer address
    MemoryCell cell = (MemoryCell)t.getTableView().getItems().get(t.getTablePosition().getRow());
    String newValue = t.getNewValue().trim();
    // convert input to an integer value
    try {
      // user enters a hex
      if (newValue.matches("^0[xX][0-9a-fA-F]+$"))
        Globals.memory.storeByte(cell.getIntAddress() + offset, Integer.parseInt(newValue.substring(2), 16));
      // user enters a decimal
      else
        Globals.memory.storeByte(cell.getIntAddress() + offset, Integer.parseInt(newValue));
    } catch (Exception e) {
      Message.warning("invalid memory cell value: " + newValue);
    }
    // always refresh table
    Platform.runLater(() -> this.memTable.refresh());
  }

  /**
   * Updates an editable rvi register cell column.
   *
   * @param t cell edit event
   */
  private void updateRVIRegister(CellEditEvent<Register, String> t) {
    // get register to set value
    Register reg = (Register)t.getTableView().getItems().get(t.getTablePosition().getRow());
    // get user input
    String newValue = t.getNewValue().trim();
    try {
      // user enters a hex
      if (newValue.matches("^0[xX][0-9a-fA-F]+$"))
        reg.setValue(Integer.parseInt(newValue.substring(2), 16));
      // user enters a binary
      else if (newValue.matches("^0[bB][01]+$"))
        reg.setValue(Integer.parseInt(newValue.substring(2), 2));
      // user enters a decimal
      else
        reg.setValue(Integer.parseInt(newValue));
    } catch (Exception e) {
      Message.warning("invalid register value: " + newValue);
    }
    // always refresh table
    Platform.runLater(() -> this.rviTable.refresh());
  }

  /**
   * Updates an editable rvf register cell column.
   *
   * @param t cell edit event
   */
  private void updateRVFRegister(CellEditEvent<Register, String> t) {
    // get register to set value
    Register reg = (Register)t.getTableView().getItems().get(t.getTablePosition().getRow());
    // get user input
    String newValue = t.getNewValue().trim();
    try {
      // user enters a hex value
      if (newValue.matches("^0[xX][0-9a-fA-F]+$"))
        reg.setValue(Integer.parseInt(newValue));
      // user enters a float
      else
        reg.setValue(Float.floatToIntBits(Float.parseFloat(newValue)));
    } catch (Exception e) {
      Message.warning("invalid register value: " + newValue);
    }
    Platform.runLater(() -> this.rvfTable.refresh());
  }


  /*-------------------------------------------------------*
   |                       Inits                           |
   *-------------------------------------------------------*/

  /**
   * This method initializes simulator control buttons.
   */
  private void initButtons() {
    this.goBtn.setOnAction(e -> this.go());
    this.goBtn.disableProperty().bind(Bindings.or(Status.EXIT, Status.RUNNING));
    this.stopBtn.setOnAction(e -> this.stop());
    this.stopBtn.disableProperty().bind(Bindings.not(Status.RUNNING));
    this.stepBtn.setOnAction(e -> this.step());
    this.stepBtn.disableProperty().bind(Bindings.or(Status.EXIT, Status.RUNNING));
    this.backstepBtn.setOnAction(e -> this.backstep());
    this.backstepBtn.disableProperty().bind(Bindings.or(Status.EMPTY, Bindings.or(Status.EXIT, Status.RUNNING)));
    this.resetBtn.setOnAction(e -> this.reset());
    this.resetBtn.disableProperty().bind(Bindings.or(Status.RUNNING, Status.EMPTY));
    this.dumpBtn.setOnAction(e -> this.dump());
    this.dumpBtn.disableProperty().bind(Status.RUNNING);
  }

  /**
   * This method initializes the text segment table.
   */
  @SuppressWarnings("unchecked")
  private void initText() {
    this.textTable.getStyleClass().add("text-table");
    // PC current row
    Globals.regfile.programCounterProperty().addListener(e -> {
      if (!Status.RUNNING.get()) {
        Platform.runLater(() -> {
          // refresh table to fire updateItem in table row
          this.textTable.refresh();
          int pc = (Globals.regfile.getProgramCounter() - MemorySegments.TEXT_SEGMENT_BEGIN) / LENGTH;
          int row = Math.min(pc, this.textTable.getItems().size() - 1);
          int first = this.vflow.getFirstVisibleCell().getIndex();
          int last = this.vflow.getLastVisibleCell().getIndex();
          if (row >= last || row <= first)
            this.textTable.scrollTo(row);
        });
      }
    });
    // apply style to row with address = current PC
    PseudoClass currentPc = PseudoClass.getPseudoClass("pc");
    this.textTable.setRowFactory(e -> {
      return new TableRow<InfoStatement>(){
        @Override
        public void updateItem(InfoStatement stmt, boolean isEmpty) {
          super.updateItem(stmt, isEmpty);
          if (isEmpty || stmt == null || Status.RUNNING.get()) {
            this.pseudoClassStateChanged(currentPc, false);
            return;
          }
          String address = stmt.getAddress();
          String pc = String.format("0x%08x", Globals.regfile.getProgramCounter());
          if (pc.equals(address))
            this.pseudoClassStateChanged(currentPc, true);
          else
            this.pseudoClassStateChanged(currentPc, false);
        }
      };
    });
    // default cell factory
    Callback<TableColumn<InfoStatement, String>,
      TableCell<InfoStatement, String>> cellFactory
          = (TableColumn<InfoStatement, String> p) -> new InfoCell();
    this.txtAddrCol.setCellValueFactory(new PropertyValueFactory<InfoStatement, String>("address"));
    this.txtAddrCol.setCellFactory(cellFactory);
    this.txtMachineCode.setCellValueFactory(new PropertyValueFactory<InfoStatement, String>("machineCode"));
    this.txtMachineCode.setCellFactory(cellFactory);
    this.txtSourceCode.setCellValueFactory(new PropertyValueFactory<InfoStatement, String>("sourceCode"));
    this.txtSourceCode.setCellFactory(cellFactory);
    this.txtBasicCode.setCellValueFactory(new PropertyValueFactory<InfoStatement, String>("basicCode"));
    this.txtBasicCode.setCellFactory(cellFactory);
    // default cell factory for breakpoint col
    Callback<TableColumn<InfoStatement, Boolean>,
      TableCell<InfoStatement, Boolean>> boolCellFactory
          = (TableColumn<InfoStatement, Boolean> p) -> new BooleanCell();
    this.txtBkptCol.setCellValueFactory(new PropertyValueFactory<InfoStatement, Boolean>("breakpoint"));
    this.txtBkptCol.setCellFactory(boolCellFactory);
  }

  /**
   * This method initializes the memory table.
   */
  @SuppressWarnings("unchecked")
  private void initMemory() {
    // cell factory
    Callback<TableColumn<MemoryCell, String>,
      TableCell<MemoryCell, String>> cellFactory
          = (TableColumn<MemoryCell, String> p) -> new MemoryEditingCell();
    // set memory columns
    this.memAddress.setCellValueFactory(new PropertyValueFactory<MemoryCell, String>("address"));
    this.memAddress.getStyleClass().add("editable-cell");
    this.memOffset0.setCellValueFactory(new PropertyValueFactory<MemoryCell, String>("offset0"));
    this.memOffset0.setCellFactory(cellFactory);
    this.memOffset0.getStyleClass().add("editable-cell");
    this.memOffset0.setOnEditCommit(e -> this.updateMemoryCell(e));
    this.memOffset1.setCellValueFactory(new PropertyValueFactory<MemoryCell, String>("offset1"));
    this.memOffset1.setCellFactory(cellFactory);
    this.memOffset1.getStyleClass().add("editable-cell");
    this.memOffset1.setOnEditCommit(e -> this.updateMemoryCell(e));
    this.memOffset2.setCellValueFactory(new PropertyValueFactory<MemoryCell, String>("offset2"));
    this.memOffset2.setCellFactory(cellFactory);
    this.memOffset2.getStyleClass().add("editable-cell");
    this.memOffset2.setOnEditCommit(e -> this.updateMemoryCell(e));
    this.memOffset3.setCellValueFactory(new PropertyValueFactory<MemoryCell, String>("offset3"));
    this.memOffset3.setCellFactory(cellFactory);
    this.memOffset3.getStyleClass().add("editable-cell");
    this.memOffset3.setOnEditCommit(e -> this.updateMemoryCell(e));
    this.memTable.setItems(Globals.memory.getCells());
    // Memory Segment Actions
    this.upBtn.setOnAction(e -> Globals.memory.up());
    this.downBtn.setOnAction(e -> Globals.memory.down());
    // Set memory table context menu
    MenuItem text = new MenuItem("Jump To Text Segment");
    text.setOnAction(e -> Globals.memory.text());
    text.setGraphic(Icons.getImage("jump"));
    MenuItem data = new MenuItem("Jump To Data Segment");
    data.setOnAction(e -> Globals.memory.data());
    data.setGraphic(Icons.getImage("jump"));
    MenuItem heap = new MenuItem("Jump To Heap Segment");
    heap.setOnAction(e -> Globals.memory.heap());
    heap.setGraphic(Icons.getImage("jump"));
    MenuItem stack = new MenuItem("Jump To Stack Segment");
    stack.setOnAction(e -> Globals.memory.stack());
    stack.setGraphic(Icons.getImage("jump"));
    MenuItem ascii = new MenuItem("ASCII Display Mode");
    ascii.setOnAction(e -> this.memoryDisplayAscii());
    ascii.setGraphic(Icons.getImage("ascii"));
    MenuItem hex = new MenuItem("Hex Display Mode");
    hex.setOnAction(e -> this.memoryDisplayHex());
    hex.setGraphic(Icons.getImage("hex"));
    MenuItem decimal = new MenuItem("Decimal Display Mode");
    decimal.setOnAction(e -> this.memoryDisplayDecimal());
    decimal.setGraphic(Icons.getImage("decimal"));
    ContextMenu menu = new ContextMenu();
    menu.getItems().addAll(text, data, heap, stack, new SeparatorMenuItem(), hex, decimal, ascii);
    this.memTable.setContextMenu(menu);
    /*
      Align table columns

      Ref:
      https://stackoverflow.com/questions/37423748/javafx-tablecolumns-headers-not-aligned-with-cells-due-to-vertical-scrollbar
    */
    Platform.runLater(() -> this.memTable.refresh());
    // disable table columns reordering (hacky and ugly)
    this.memTable.getColumns().addListener(new ListChangeListener() {
      @Override
      public void onChanged(Change change) {
        change.next();
        if (change.wasReplaced()) {
          memTable.getColumns().clear();
          memTable.getColumns().addAll(memAddress, memOffset0, memOffset1, memOffset2, memOffset3);
        }
      }
    });
  }

  /**
   * This method initializes RVI and RVF tables.
   */
  @SuppressWarnings("unchecked")
  private void initRegFiles() {
    // cell factory
    Callback<TableColumn<Register, String>,
      TableCell<Register, String>> cellFactory
          = (TableColumn<Register, String> p) -> new RegisterEditingCell();
    // RVI table
    this.rviMnemonic.setCellValueFactory(new PropertyValueFactory<Register, String>("mnemonic"));
    this.rviMnemonic.getStyleClass().add("editable-cell");
    this.rviNumber.setCellValueFactory(new PropertyValueFactory<Register, Integer>("number"));
    this.rviNumber.getStyleClass().add("editable-cell");
    this.rviValue.setCellValueFactory(new PropertyValueFactory<Register, String>("strValue"));
    this.rviValue.getStyleClass().add("editable-cell");
    this.rviValue.setCellFactory(cellFactory);
    this.rviValue.setOnEditCommit(e -> this.updateRVIRegister(e));
    // apply style to modified rvi register
    ObservableList<Register> rviList = Globals.regfile.getRVI();
    for (Register reg: rviList) {
      reg.setOnSetValueListener(number -> {
        lastReg = reg;
        if (!Status.RUNNING.get()) {
          Platform.runLater(() -> {
            hardware.getSelectionModel().select(rviTab);
            int first = rviVFlow.getFirstVisibleCell().getIndex();
            int last = rviVFlow.getLastVisibleCell().getIndex();
            if (number >= last || number <= first)
              rviTable.scrollTo(number);
            rviTable.refresh();
          });
        }
      });
    }
    PseudoClass changed = PseudoClass.getPseudoClass("changed");
    this.rviTable.setRowFactory(e -> {
      return new TableRow<Register>() {
        @Override
        public void updateItem(Register reg, boolean isEmpty) {
          super.updateItem(reg, isEmpty);
          if (isEmpty || reg == null || lastReg == null || Status.RUNNING.get()) {
            this.pseudoClassStateChanged(changed, false);
            return;
          }
          this.pseudoClassStateChanged(changed, reg == lastReg);
        }
      };
    });
    this.rviTable.setItems(rviList);
    // RVF table
    this.rvfMnemonic.setCellValueFactory(new PropertyValueFactory<Register, String>("mnemonic"));
    this.rvfMnemonic.getStyleClass().add("editable-cell");
    this.rvfNumber.setCellValueFactory(new PropertyValueFactory<Register, Integer>("number"));
    this.rvfNumber.getStyleClass().add("editable-cell");
    this.rvfValue.setCellValueFactory(new PropertyValueFactory<Register, String>("strValue"));
    this.rvfValue.getStyleClass().add("editable-cell");
    this.rvfValue.setCellFactory(cellFactory);
    this.rvfValue.setOnEditCommit(e -> this.updateRVFRegister(e));
    // apply style to modified rvf register
    ObservableList<Register> rvfList = Globals.fregfile.getRVF();
    for (Register reg: rvfList) {
      reg.setOnSetValueListener(number -> {
        lastReg = reg;
        if (!Status.RUNNING.get()) {
          Platform.runLater(() -> {
            hardware.getSelectionModel().select(rvfTab);
            int first = rvfVFlow.getFirstVisibleCell().getIndex();
            int last = rvfVFlow.getLastVisibleCell().getIndex();
            if (number >= last || number <= first)
              rvfTable.scrollTo(number);
            rvfTable.refresh();
          });
        }
      });
    }
    this.rvfTable.setRowFactory(e -> {
      return new TableRow<Register>() {
        @Override
        public void updateItem(Register reg, boolean isEmpty) {
          super.updateItem(reg, isEmpty);
          if (isEmpty || reg == null || lastReg == null || Status.RUNNING.get()) {
            this.pseudoClassStateChanged(changed, false);
            return;
          }
          this.pseudoClassStateChanged(changed, reg == lastReg);
        }
      };
    });
    this.rvfTable.setItems(rvfList);
    // set rvi table context menu
    MenuItem hex = new MenuItem("Hex Display Mode");
    hex.setOnAction(e -> this.rviDisplayHex());
    hex.setGraphic(Icons.getImage("hex"));
    MenuItem decimal = new MenuItem("Decimal Display Mode");
    decimal.setOnAction(e -> this.rviDisplayDecimal());
    decimal.setGraphic(Icons.getImage("decimal"));
    MenuItem unsigned = new MenuItem("Unsigned Display Mode");
    unsigned.setOnAction(e -> this.rviDisplayUnsigned());
    unsigned.setGraphic(Icons.getImage("unsigned"));
    ContextMenu menu = new ContextMenu();
    menu.getItems().addAll(hex, decimal, unsigned);
    this.rviTable.setContextMenu(menu);
    // set rvf table context menu
    hex = new MenuItem("Hex Display Mode");
    hex.setOnAction(e -> this.rvfDisplayHex());
    hex.setGraphic(Icons.getImage("hex"));
    MenuItem dfloat = new MenuItem("Float Display Mode");
    dfloat.setOnAction(e -> this.rvfDisplayFloat());
    dfloat.setGraphic(Icons.getImage("float"));
    menu = new ContextMenu();
    menu.getItems().addAll(hex, dfloat);
    this.rvfTable.setContextMenu(menu);
    /*
      Align table columns

      Ref:
      https://stackoverflow.com/questions/37423748/javafx-tablecolumns-headers-not-aligned-with-cells-due-to-vertical-scrollbar
    */
    Platform.runLater(() -> {
      this.rviTable.refresh();
      this.rvfTable.refresh();
      // get virtual flows
      this.rviVFlow = (VirtualFlow<?>)((TableViewSkin<?>)this.rviTable.getSkin()).getChildren().get(1);
      this.rvfVFlow = (VirtualFlow<?>)((TableViewSkin<?>)this.rvfTable.getSkin()).getChildren().get(1);
    });
    // disable table columns reordering (hacky and ugly)
    this.rviTable.getColumns().addListener(new ListChangeListener() {
      @Override
      public void onChanged(Change change) {
        change.next();
        if (change.wasReplaced()) {
          rviTable.getColumns().clear();
          rviTable.getColumns().addAll(rviMnemonic, rviNumber, rviValue);
        }
      }
    });
    this.rvfTable.getColumns().addListener(new ListChangeListener() {
      @Override
      public void onChanged(Change change) {
        change.next();
        if (change.wasReplaced()) {
          rvfTable.getColumns().clear();
          rvfTable.getColumns().addAll(rvfMnemonic, rvfNumber, rvfValue);
        }
      }
    });
  }

}
