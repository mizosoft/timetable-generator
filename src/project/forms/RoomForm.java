/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package project.forms;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import project.entities.Classroom;
import project.entities.Laboratory;
import project.entities.Room;

public class RoomForm extends JDialog implements ActionListener {

  Color buttoncolor;
  final int x_position = 50;
  JRadioButton Lecture;
  JRadioButton Lab;
  JButton AddButton;
  final ButtonGroup ButtonItem;

  final JLabel ID;
  final JLabel Name;
  final JLabel NumOfChairs;

  private final JLabel Duratio;

  JTextField ID_field;
  JTextField Name_field;
  JTextField NumOfChairs_field;

  private JTextField Duration_Feild;

  private Room room;

  private final List<Room> addedRooms;

  public RoomForm(JFrame owner, List<Room> addedRooms) {
    super(owner, true);

    this.addedRooms = addedRooms;

    this.setTitle("Fill Room Information");
    this.setBackground(new Color(55, 75, 110));
    getContentPane().setLayout(null);
    this.setSize(350, 500);
    buttoncolor = new Color(240, 92, 47);

    Lecture = new JRadioButton("Lecture");
    Lecture.setFont(new Font("Eras Demi ITC", Font.PLAIN, 14));
    Lecture.setBackground(buttoncolor);
    Lecture.setForeground(Color.white);
    Lab = new JRadioButton("Lab");
    Lab.setFont(new Font("Eras Demi ITC", Font.PLAIN, 14));
    Lab.setBackground(buttoncolor);
    Lab.setForeground(Color.white);
    AddButton = new JButton("Add");
    AddButton.setBackground(buttoncolor);
    AddButton.setForeground(Color.white);
    ButtonItem = new ButtonGroup();
    ButtonItem.add(Lecture);
    ButtonItem.add(Lab);

    // set label color and font
    ID = new JLabel("Hall ID");
    ID.setFont(new Font("Eras Demi ITC", Font.PLAIN, 12));
    ID.setForeground(Color.white);
    Name = new JLabel("Hall Name");
    Name.setFont(new Font("Eras Demi ITC", Font.PLAIN, 12));
    Name.setForeground(Color.white);
    NumOfChairs = new JLabel("Number Of chairs");
    NumOfChairs.setFont(new Font("Eras Demi ITC", Font.PLAIN, 12));
    NumOfChairs.setForeground(Color.white);

    Duratio = new JLabel("Duration");
    Duratio.setFont(new Font("Eras Demi ITC", Font.PLAIN, 12));
    Duratio.setForeground(Color.white);

    ID_field = new JTextField();
    Name_field = new JTextField();
    NumOfChairs_field = new JTextField();

    Duration_Feild = new JTextField();

    // set item size and location
    Lecture.setBounds(x_position, 50, 100, 20);
    Lab.setBounds(200, 50, 100, 20);
    ID.setBounds(x_position, 100, 80, 70);
    ID_field.setBounds(160, 125, 100, 20);
    Name.setBounds(x_position, 140, 80, 70);
    Name_field.setBounds(160, 160, 100, 20);
    NumOfChairs.setBounds(x_position, 180, 120, 70);
    NumOfChairs_field.setBounds(160, 205, 100, 20);

    Duratio.setBounds(x_position, 340, 100, 80);
    Duration_Feild.setBounds(160, 370, 100, 20);
    AddButton.setBounds(100, 420, 100, 20);

    AddButton.addActionListener(this);

    // add item to Jframe
    getContentPane().add(Lecture);
    getContentPane().add(ID);
    getContentPane().add(Lab);
    getContentPane().add(ID_field);
    getContentPane().add(Name);
    getContentPane().add(Name_field);
    getContentPane().add(NumOfChairs);
    getContentPane().add(NumOfChairs_field);

    getContentPane().add(Duratio);
    getContentPane().add(Duration_Feild);
    getContentPane().add(AddButton);
    this.getContentPane().setBackground(new Color(55, 55, 110));

    this.setResizable(false);
    this.setLocation(500, 90);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    System.out.println("Adding");
    if (ID_field.getText().isBlank()
        || Name_field.getText().isBlank()
        || NumOfChairs_field.getText().isBlank()) {
      JOptionPane.showMessageDialog(
          this,
          "All fields must be filled",
          "Missing fields",
          JOptionPane.ERROR_MESSAGE);
    } else {
      if (hasRoomWithSameId(Integer.parseInt(ID_field.getText()))) {
        JOptionPane.showMessageDialog(
            this,
            "A room with the given ID is already present",
            "Invalid",
            JOptionPane.ERROR_MESSAGE);
        return;
      }

      if (!Lecture.isSelected() && !Lab.isSelected()) {
        JOptionPane.showMessageDialog(
            this,
            "All fields must be filled",
            "Missing fields",
            JOptionPane.ERROR_MESSAGE);
      } else if (Lecture.isSelected()) {
        room =
            new Classroom.Builder()
                .setId(Integer.parseInt(ID_field.getText()))
                .setName(Name_field.getText())
                .setNumberOfChairs(Integer.parseInt(NumOfChairs_field.getText()))
                .build();

        dispose();
      } else if (Lab.isSelected()) {
        room =
            new Laboratory.Builder()
                .setId(Integer.parseInt(ID_field.getText()))
                .setName(Name_field.getText())
                .setNumberOfChairs(Integer.parseInt(NumOfChairs_field.getText()))
                .build();

        dispose();
      }
    }
  }

  private boolean hasRoomWithSameId(int id) {
    return addedRooms.stream().anyMatch(room -> room.getId() == id);
  }

  Room getInsertedRoom() {
    return room;
  }
}
