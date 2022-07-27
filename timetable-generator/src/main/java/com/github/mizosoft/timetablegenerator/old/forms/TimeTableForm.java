/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.mizosoft.timetablegenerator.old.forms;

import com.github.mizosoft.timetablegenerator.old.Utils;
import com.github.mizosoft.timetablegenerator.old.Utils.Pair;
import com.github.mizosoft.timetablegenerator.old.model.Course;
import com.github.mizosoft.timetablegenerator.old.model.CourseAttendable;
import com.github.mizosoft.timetablegenerator.old.model.DaySchedule;
import com.github.mizosoft.timetablegenerator.old.model.Room;
import com.github.mizosoft.timetablegenerator.old.model.Teacher;
import com.github.mizosoft.timetablegenerator.old.model.TimeRange;
import com.github.mizosoft.timetablegenerator.old.model.Timetable;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

/** @author moataz */
public class TimeTableForm extends javax.swing.JFrame {

  private static final int TIME_TABLE_TAB = 0;
  private static final int COURSES_TAB = 1;
  private static final int ROOMS_TAB = 2;
  private static final int TEACHERS_TAB = 3;

  private final List<Course> courses = new ArrayList<>();
  private final List<Room> rooms = new ArrayList<>();
  private final List<Teacher> teachers = new ArrayList<>();

  /** Creates new form TimeTableForm */
  public TimeTableForm() {
    initComponents();

    timeTable.setModel(EmptyTimeTableModel.INSTANCE);
    coursesTable.setModel(new CoursesTableModel(List.of()));
    attendablesTable.setModel(new CourseAttendableTableModel(List.of()));
    teachersTable.setModel(new TeachersTableModel(List.of()));
    roomsTable.setModel(new RoomsTableModel(List.of()));

    timeTable.setDefaultRenderer(Pair.class, new TimeTableCellRenderer());

    setLocationRelativeTo(null);
    setResizable(false);
  }

  //    void dummyCourse() {
  //        timeTable.setDefaultRenderer(Pair.class, new TimeTableCellRenderer());
  //        var t1 = new Teacher.Builder()
  //                .setName("Mr Alfons")
  //                .build();
  //        var t2 = new Teacher.Builder()
  //                .setName("Mr Shakal")
  //                .build();
  //        var r1 = new Laboratory.Builder()
  //                .setName("Odet lfran")
  //                .setId(3)
  //                .build();
  //         var r2 = new Laboratory.Builder()
  //                .setName("W.C")
  //                .setId(2)
  //                .build();
  //         var a1 = new Lab.Builder()
  //                 .setDuration(Duration.ofHours(2))
  //                 .setRoom(r1)
  //                 .setTeacher(t1).build();
  //         var a2 = new Lecture.Builder()
  //                 .setDuration(Duration.ofHours(4))
  //                 .setRoom(r2)
  //                 .setTeacher(t1).build();
  //         var a3= new Lecture.Builder()
  //                 .setDuration(Duration.ofHours(2))
  //                 .setRoom(r1)
  //                 .setTeacher(t2).build();
  //         var a4= new Lab.Builder()
  //                 .setDuration(Duration.ofHours(2))
  //                 .setRoom(r1)
  //                 .setTeacher(t2).build();
  //         var a5= new Lab.Builder()
  //                 .setDuration(Duration.ofHours(2))
  //                 .setRoom(r1)
  //                 .setTeacher(t1).build();
  //         var a6 = new Lecture.Builder()
  //                 .setDuration(Duration.ofHours(6))
  //                 .setRoom(r2)
  //                 .setTeacher(t1).build();
  //         var a7= new Lab.Builder()
  //                 .setDuration(Duration.ofHours(4))
  //                 .setRoom(r1)
  //                 .setTeacher(t1).build();
  //         var a8= new Lab.Builder()
  //                 .setDuration(Duration.ofHours(1))
  //                 .setRoom(r2)
  //                 .setTeacher(t2).build();
  //         var a9= new Lecture.Builder()
  //                 .setDuration(Duration.ofHours(3))
  //                 .setRoom(r1)
  //                 .setTeacher(t2).build();
  //         var a10= new Lab.Builder()
  //                 .setDuration(Duration.ofHours(2))
  //                 .setRoom(r2)
  //                 .setTeacher(t1).build();
  //
  //         var course = new Course.Builder()
  //                 .setId(1)
  //                 .setName("Math 4")
  //                 .addAttendables(a1, a3, a4, a8)
  //                 .build();
  //         var c2 = new Course.Builder()
  //                 .setId(2)
  //                 .setName("Art")
  //                 .addAttendables(a2, a6, a7, a5, a9, a10).build();
  //         courses.addAll(List.of(course, c2));
  //         timeTable.setModel(new TimeTableModel(TimeTable.create(courses)));
  //    }

  /**
   * This method is called from within the constructor to initialize the form. WARNING: Do NOT
   * modify this code. The content of this method is always regenerated by the Form Editor.
   */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonPanel = new javax.swing.JPanel();
        makeTableButton = new javax.swing.JButton();
        addButton = new javax.swing.JButton();
        clearButton = new javax.swing.JButton();
        removeButton = new javax.swing.JButton();
        resetTableButton = new javax.swing.JButton();
        tabsPane = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        timeTable = new javax.swing.JTable();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        coursesTable = new javax.swing.JTable();
        jScrollPane4 = new javax.swing.JScrollPane();
        attendablesTable = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        roomsTable = new javax.swing.JTable();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        teachersTable = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        makeTableButton.setText("Make Table");
        makeTableButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                makeTableButtonActionPerformed(evt);
            }
        });

        addButton.setText("Add");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        clearButton.setText("Clear");
        clearButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearButtonActionPerformed(evt);
            }
        });

        removeButton.setText("Remove");
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeButtonActionPerformed(evt);
            }
        });

        resetTableButton.setText("Reset Table");
        resetTableButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetTableButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout buttonPanelLayout = new javax.swing.GroupLayout(buttonPanel);
        buttonPanel.setLayout(buttonPanelLayout);
        buttonPanelLayout.setHorizontalGroup(
            buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(makeTableButton, javax.swing.GroupLayout.DEFAULT_SIZE, 138, Short.MAX_VALUE)
            .addComponent(addButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(clearButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(removeButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(resetTableButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        buttonPanelLayout.setVerticalGroup(
            buttonPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, buttonPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(makeTableButton, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(resetTableButton, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(addButton, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(removeButton, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(clearButton, javax.swing.GroupLayout.PREFERRED_SIZE, 58, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        timeTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane5.setViewportView(timeTable);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 790, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 335, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabsPane.addTab("Time Table", jPanel1);

        coursesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        coursesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                coursesTableMouseClicked(evt);
            }
        });
        jScrollPane3.setViewportView(coursesTable);

        attendablesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane4.setViewportView(attendablesTable);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 790, Short.MAX_VALUE)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 790, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 165, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabsPane.addTab("Courses", jPanel2);

        roomsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(roomsTable);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 790, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 335, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabsPane.addTab("Rooms", jPanel3);

        teachersTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(teachersTable);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 790, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 335, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabsPane.addTab("Teachers", jPanel4);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tabsPane)
                .addGap(18, 18, 18)
                .addComponent(buttonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(buttonPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(tabsPane))
                .addGap(0, 10, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void coursesTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_coursesTableMouseClicked
        updateAttendablesTableModel();
    }//GEN-LAST:event_coursesTableMouseClicked

  private void makeTableButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_makeTableButtonActionPerformed
    if (tabsPane.getSelectedIndex() == TIME_TABLE_TAB) {
      if (courses.isEmpty()) {
        JOptionPane.showMessageDialog(
            this, "No courses are added", "Invalid state", JOptionPane.ERROR_MESSAGE);
        return;
      }

      var table = Timetable.create(courses);
      timeTable.setModel(new TimeTableModel(table));
    }
  } // GEN-LAST:event_makeTableButtonActionPerformed

  private void addButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_addButtonActionPerformed
    switch (tabsPane.getSelectedIndex()) {
      case COURSES_TAB:
        var course = promptCourse();
        if (course != null) {
          courses.add(course);
          coursesTable.setModel(new CoursesTableModel(List.copyOf(courses)));
          updateAttendablesTableModel();
        }
        break;

      case ROOMS_TAB:
        var room = promptRoom();
        if (room != null) {
          rooms.add(room);
          roomsTable.setModel(new RoomsTableModel(List.copyOf(rooms)));
        }
        break;

      case TEACHERS_TAB:
        var teacher = promptTeacher();
        if (teacher != null) {
          teachers.add(teacher);
          teachersTable.setModel(new TeachersTableModel(List.copyOf(teachers)));
        }
        break;

      default:
        {
        }
    }
  } // GEN-LAST:event_addButtonActionPerformed

  private void clearButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_clearButtonActionPerformed
    switch (tabsPane.getSelectedIndex()) {
      case COURSES_TAB:
        courses.clear();
        coursesTable.setModel(new CoursesTableModel(List.of()));
        updateAttendablesTableModel();
        break;

      case ROOMS_TAB:
        rooms.clear();
        roomsTable.setModel(new RoomsTableModel(List.of()));
        break;

      case TEACHERS_TAB:
        teachers.clear();
        teachersTable.setModel(new TeachersTableModel(List.of()));
        break;
    }
  } // GEN-LAST:event_clearButtonActionPerformed

  private void removeButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_removeButtonActionPerformed
    switch (tabsPane.getSelectedIndex()) {
      case COURSES_TAB:
        removeSelected(courses, coursesTable, CoursesTableModel::new);
        updateAttendablesTableModel();
        break;

      case ROOMS_TAB:
        removeSelected(rooms, roomsTable, RoomsTableModel::new);
        break;

      case TEACHERS_TAB:
        removeSelected(teachers, teachersTable, TeachersTableModel::new);
        break;
    }
  } // GEN-LAST:event_removeButtonActionPerformed

  private <T> void removeSelected(
      List<T> list, JTable table, Function<List<T>, AbstractTypedTableModel<T>> modelSupplier) {
    for (int index : table.getSelectedRows()) {
      if (index >= 0 && index < list.size()) {
        list.remove(index);
      }
    }

    table.setModel(modelSupplier.apply(list));
  }

  private void resetTableButtonActionPerformed(
      java.awt.event.ActionEvent evt) { // GEN-FIRST:event_resetTableButtonActionPerformed
    if (tabsPane.getSelectedIndex() == TIME_TABLE_TAB) {
      timeTable.setModel(EmptyTimeTableModel.INSTANCE);
    }
  } // GEN-LAST:event_resetTableButtonActionPerformed

  private Teacher promptTeacher() {
    var teacherForm = new TeacherForm(this, List.copyOf(teachers));
    teacherForm.setVisible(true);
    return teacherForm.getInsertedTeacher();
  }

  private Room promptRoom() {
    var roomForm = new RoomForm(this, List.copyOf(rooms));
    roomForm.setVisible(true);
    return roomForm.getInsertedRoom();
  }

  private Course promptCourse() {
    if (teachers.isEmpty() || rooms.isEmpty()) {
      JOptionPane.showMessageDialog(this,
          "No rooms or teachers were previously added",
          "Invalid state",
          JOptionPane.ERROR_MESSAGE);
      return null;
    }

    var courseForm = new CourseForm(
        this, List.copyOf(rooms), List.copyOf(teachers), List.copyOf(courses));
    courseForm.setVisible(true);
    return courseForm.getInsertedCourse();
  }

  private void updateAttendablesTableModel() {
    var index = coursesTable.getSelectedRow();
    if (index >= 0 && index < courses.size()) {
      attendablesTable.setModel(new CourseAttendableTableModel(courses.get(index).getAttendables()));
    }
  }

  /** @param args the command line arguments */
  public static void main(String args[]) {
    /* Set the Nimbus look and feel */
    // <editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
    /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
     * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
     */
    try {
      for (javax.swing.UIManager.LookAndFeelInfo info :
          javax.swing.UIManager.getInstalledLookAndFeels()) {
        if ("Metal".equals(info.getName())) {
          javax.swing.UIManager.setLookAndFeel(info.getClassName());
          break;
        }
      }
    } catch (ClassNotFoundException ex) {
      java.util.logging.Logger.getLogger(TimeTableForm.class.getName())
          .log(java.util.logging.Level.SEVERE, null, ex);
    } catch (InstantiationException ex) {
      java.util.logging.Logger.getLogger(TimeTableForm.class.getName())
          .log(java.util.logging.Level.SEVERE, null, ex);
    } catch (IllegalAccessException ex) {
      java.util.logging.Logger.getLogger(TimeTableForm.class.getName())
          .log(java.util.logging.Level.SEVERE, null, ex);
    } catch (javax.swing.UnsupportedLookAndFeelException ex) {
      java.util.logging.Logger.getLogger(TimeTableForm.class.getName())
          .log(java.util.logging.Level.SEVERE, null, ex);
    }
    // </editor-fold>

    /* Create and display the form */
    java.awt.EventQueue.invokeLater(
        () -> {
          new TimeTableForm().setVisible(true);
        });
  }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JTable attendablesTable;
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JButton clearButton;
    private javax.swing.JTable coursesTable;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JButton makeTableButton;
    private javax.swing.JButton removeButton;
    private javax.swing.JButton resetTableButton;
    private javax.swing.JTable roomsTable;
    private javax.swing.JTabbedPane tabsPane;
    private javax.swing.JTable teachersTable;
    private javax.swing.JTable timeTable;
    // End of variables declaration//GEN-END:variables

  private abstract static class AbstractTimeTableModel extends AbstractTableModel {
    private static final String[] STUDY_DAY_ABBREVIATIONS = new String[Timetable.STUDY_DAYS.length];

    static {
      for (int i = 0; i < Timetable.STUDY_DAYS.length; i++) {
        var str = Timetable.STUDY_DAYS[i].toString();
        STUDY_DAY_ABBREVIATIONS[i] =
            Character.toUpperCase(str.charAt(0)) + str.substring(1, 3).toLowerCase();
      }
    }

    AbstractTimeTableModel() {}

    @Override
    public int getColumnCount() {
      return Timetable.STUDY_DAYS.length;
    }

    @Override
    public String getColumnName(int column) {
      return STUDY_DAY_ABBREVIATIONS[column];
    }
  }

  private static final class EmptyTimeTableModel extends AbstractTimeTableModel {
    static final EmptyTimeTableModel INSTANCE = new EmptyTimeTableModel();

    private EmptyTimeTableModel() {}

    @Override
    public int getRowCount() {
      return 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      return "";
    }
  }

  private static final class TimeTableModel extends AbstractTimeTableModel {
    private final Timetable table;
    private final int rowCount;

    TimeTableModel(Timetable timeTable) {
      this.table = timeTable;
      rowCount = timeTable.getTable().values().stream().mapToInt(DaySchedule::size).max().orElse(0);
    }

    @Override
    public int getRowCount() {
      return rowCount;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      var daySchedule = table.getTable().get(Timetable.STUDY_DAYS[columnIndex]);
      return daySchedule != null && daySchedule.size() > rowIndex
          ? daySchedule.getScheduleList().get(rowIndex)
          : null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return Pair.class;
    }
  }

  private static final class TimeTableCellRenderer implements TableCellRenderer {

    private final List<List<Integer>> rowColHeight = new ArrayList<>();

    TimeTableCellRenderer() {}

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

      var textArea = new JTextArea();
      textArea.setLineWrap(true);
      textArea.setWrapStyleWord(true);
      textArea.setOpaque(true);

      //            if (isSelected) {
      //                textArea.setForeground(table.getSelectionForeground());
      //                textArea.setBackground(table.getSelectionBackground());
      //            } else {
      //                textArea.setForeground(table.getForeground());
      //                textArea.setBackground(table.getBackground());
      //            }

      textArea.setFont(table.getFont());
      if (hasFocus) {
        textArea.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
        if (table.isCellEditable(row, column)) {
          textArea.setForeground(UIManager.getColor("Table.focusCellForeground"));
          textArea.setBackground(UIManager.getColor("Table.focusCellBackground"));
        }
      } else {
        textArea.setBorder(new EmptyBorder(1, 2, 1, 2));
      }

      if (value != null) {
        var str = new TimeTableCellFormatter(Utils.castUnchecked(value)).format();
        textArea.setText(str);
      } else {
        textArea.setText("");
      }

      adjustRowHeight(table, textArea, row, column);
      return textArea;
    }

    private void adjustRowHeight(JTable table, JTextArea textArea, int row, int column) {
      int cWidth = table.getTableHeader().getColumnModel().getColumn(column).getWidth();
      textArea.setSize(new Dimension(cWidth, 1000));
      int prefH = textArea.getPreferredSize().height;
      while (rowColHeight.size() <= row) {
        rowColHeight.add(new ArrayList<>(column));
      }

      var colHeights = rowColHeight.get(row);
      while (colHeights.size() <= column) {
        colHeights.add(0);
      }

      colHeights.set(column, prefH);
      int maxH = prefH;
      for (Integer colHeight : colHeights) {
        if (colHeight > maxH) {
          maxH = colHeight;
        }
      }

      if (table.getRowHeight(row) != maxH) {
        table.setRowHeight(row, maxH);
      }
    }
  }

  private static final class TimeTableCellFormatter {
    private final Pair<TimeRange, Pair<Course, CourseAttendable>> cell;

    private TimeTableCellFormatter(Pair<TimeRange, Pair<Course, CourseAttendable>> cell) {
      this.cell = cell;
    }

    String format() {
      return Stream.of(timeRangeTag(), courseTag(), roomTag(), teacherTag())
          .collect(Collectors.joining(System.lineSeparator()));
    }

    private String timeRangeTag() {
      var range = cell.getFirst();
      return String.format(
          "%s - %s", formatHour(range.getFromHour()), formatHour(range.getToHour()));
    }

    private String formatHour(int hour) {
      return hour % 12 + (hour < 12 ? "am" : "pm");
    }

    private String courseTag() {
      var attendablePair = cell.getSecond();
      return attendablePair.getFirst().getName()
          + " "
          + Utils.capitalizeEnum(attendablePair.getSecond().getType());
    }

    private String roomTag() {
      var attendable = cell.getSecond().getSecond();
      var room = attendable.getRoom();
      return room.getName() + " " + Utils.capitalizeEnum(room.getType());
    }

    private String teacherTag() {
      return cell.getSecond().getSecond().getTeacher().getName();
    }
  }

  // 8am - 10am
  // Math 4 Lecture
  // Lab Bla blah, Dr Alfons
  /*private static final class TimeTableCellModel
      extends AbstractListModel<String> {

      private static final int SIZE = 3;

      private final Pair<TimeRange, Pair<Course, CourseAttendable>> cell;

      private TimeTableCellModel(
          Pair<TimeRange, Pair<Course, CourseAttendable>> cell) {
          this.cell = cell;
      }

      @Override
      public int getSize() {
          return SIZE;
      }

      @Override
      public String getElementAt(int index) {
          switch (index) {
              case 0:
                  return timeRange();
              case 1:
                  return courseTag();
              case 3:
                  return roomAndTeacherTag();
              default:
                  return "";
          }
      }

      private String timeRange() {
          var range = cell.getFirst();
          return format("%s - %s", formatHour(range.getFromHour()), formatHour(range.getToHour()));
      }

      private String formatHour(int hour) {
          return (hour >= 12 ? hour : (hour % 12 + 1))
              + hour < 12 ? "AM" : "PM";
      }

      private String courseTag() {
          var attendablePair = cell.getSecond();
          return attendablePair.getFirst().getName()
              + " " + capitalizeEnum(attendablePair.getSecond().getType());
      }

      private String roomAndTeacherTag() {
          var attendable = cell.getSecond().getSecond();
          var room = attendable.getRoom();
          var teacher = attendable.getTeacher();
          return room.getName() + " " + capitalizeEnum(room.getType()) + ", " + teacher.getName();
      }
  }*/

  private interface ColumnType<T> {
    String name();

    Object get(T obj);

    Class<?> type();
  }

  private abstract static class AbstractTypedTableModel<T> extends AbstractTableModel {
    private final List<T> values;
    private final List<ColumnType<T>> columns;

    AbstractTypedTableModel(List<T> values, List<ColumnType<T>> columns) {
      this.values = values;
      this.columns = columns;
    }

    AbstractTypedTableModel(List<T> values, ColumnType<T>[] columns) {
      this(values, List.of(columns));
    }

    @Override
    public int getRowCount() {
      return values.size();
    }

    @Override
    public int getColumnCount() {
      return columns.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      return columns.get(columnIndex).get(values.get(rowIndex));
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return columns.get(columnIndex).type();
    }

    @Override
    public String getColumnName(int column) {
      return Utils.capitalize(columns.get(column).name());
    }
  }

  private static final class TeachersTableModel extends AbstractTypedTableModel<Teacher> {

    TeachersTableModel(List<Teacher> teachers) {
      super(teachers, TeacherType.values());
    }

    private enum TeacherType implements ColumnType<Teacher> {
      ID(int.class, Teacher::getId),
      NAME(String.class, Teacher::getName),
      EMAIL(String.class, Teacher::getEmail),
      PHONE(String.class, Teacher::getPhone);

      final Class<?> clazz;
      final Function<Teacher, ?> getter;

      <R> TeacherType(Class<R> clazz, Function<Teacher, R> getter) {
        this.clazz = clazz;
        this.getter = getter;
      }

      @Override
      public Object get(Teacher teacher) {
        return getter.apply(teacher);
      }

      @Override
      public Class<?> type() {
        return clazz;
      }
    }
  }

  private static final class RoomsTableModel extends AbstractTypedTableModel<Room> {

    RoomsTableModel(List<Room> rooms) {
      super(rooms, RoomColumn.values());
    }

    private enum RoomColumn implements ColumnType<Room> {
      ID(int.class, Room::getId),
      NAME(String.class, Room::getName),
      TYPE(String.class, room -> Utils.capitalizeEnum(room.getType())),
      NUMBER_OF_CHAIRS(int.class, Room::getNumberOfChairs);

      final Class<?> clazz;
      final Function<Room, ?> getter;

      <R> RoomColumn(Class<R> clazz, Function<Room, R> getter) {
        this.clazz = clazz;
        this.getter = getter;
      }

      @Override
      public Class<?> type() {
        return clazz;
      }

      @Override
      public Object get(Room room) {
        return getter.apply(room);
      }
    }
  }

  private static final class CoursesTableModel extends AbstractTypedTableModel<Course> {

    CoursesTableModel(List<Course> values) {
      super(values, CourseColumn.values());
    }

    private enum CourseColumn implements ColumnType<Course> {
      ID(int.class, Course::getId),
      NAME(String.class, Course::getName),
      ATTENDABLES(
          String.class,
          course -> {
            var str = "";
            var lecturesCount = course.getLectures().size();
            if (lecturesCount > 0) {
              str += lecturesCount + " Lecture" + (lecturesCount > 1 ? "s" : "");
            }

            var labsCount = course.getLabs().size();
            if (labsCount > 0) {
              str += (lecturesCount > 0 ? ", " : "")
                      + labsCount + " Lab" + (labsCount > 1 ? "s" : "");
            }

            return str;
          });

      final Class<?> clazz;
      final Function<Course, ?> getter;

      <R> CourseColumn(Class<R> clazz, Function<Course, R> getter) {
        this.clazz = clazz;
        this.getter = getter;
      }

      @Override
      public Class<?> type() {
        return clazz;
      }

      @Override
      public Object get(Course course) {
        return getter.apply(course);
      }
    }
  }

  static final class CourseAttendableTableModel
      extends AbstractTypedTableModel<CourseAttendable> {

    CourseAttendableTableModel(List<CourseAttendable> values) {
      super(values, CourseAttendableColumn.values());
    }

    private enum CourseAttendableColumn implements ColumnType<CourseAttendable> {
      TYPE(String.class, a -> Utils.capitalizeEnum(a.getType())),
      TEACHER(String.class, a -> a.getTeacher().getName()),
      ROOM(String.class, a -> a.getRoom().getName()),
      DURATION(long.class, a -> a.getDuration().toHours());

      final Class<?> clazz;
      final Function<CourseAttendable, ?> getter;

      <R> CourseAttendableColumn(Class<R> clazz, Function<CourseAttendable, R> getter) {
        this.clazz = clazz;
        this.getter = getter;
      }

      @Override
      public Object get(CourseAttendable obj) {
        return getter.apply(obj);
      }

      @Override
      public Class<?> type() {
        return clazz;
      }
    }
  }
}
