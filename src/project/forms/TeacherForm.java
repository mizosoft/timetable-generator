package project.forms;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Pattern;
import project.entities.Teacher;

public class TeacherForm extends JDialog implements ActionListener {
  private String teacherName;
  private int teacherID;
  private String teacherPhone;
  private String teacherEmail;
  private Teacher.Builder t1;
  private Color backgroundColor;
  private Color buttoncolor;
  private Pattern specChars;
  private Pattern emailChars;
  private final JLabel formTitle, name, id, phone, email;
  private static JTextField tName, tID, tPhone, tEmail;
  private static JButton saveButton, clearButton;
  private final JPanel p1;

  private Teacher teacher;
  private final java.util.List<Teacher> addedTeachers;

  public TeacherForm(JFrame owner, java.util.List<Teacher> addedTeachers) {
    super(owner, true);

    this.addedTeachers = addedTeachers;

    teacherName = "";
    teacherEmail = "";
    teacherPhone = "";
    t1 = new Teacher.Builder();
    specChars = Pattern.compile("[-$&+,:;=\\?@#|/'<>.^*()%123456789_]");
    emailChars = Pattern.compile("[$&+,:;=\\?#|/'<>^*()%]");
    backgroundColor = new Color(55, 75, 110);
    buttoncolor = new Color(240, 92, 47);
    this.setTitle("Teacher Registertion Form");

    this.setSize(650, 600);
    this.setLocation(500, 90);
    this.setResizable(false);
    p1 = new JPanel();
    p1.setBackground(backgroundColor);
    this.add(p1);
    p1.setLayout(null);

    // formTitle details
    formTitle = new JLabel("Teacher Registertion Form");
    formTitle.setFont(new Font("Eras Demi ITC", Font.BOLD, 20));
    formTitle.setForeground(Color.white);
    formTitle.setSize(300, 30);
    formTitle.setLocation(190, 40);
    p1.add(formTitle);

    // labelName details
    name = new JLabel("Name:");
    name.setFont(new Font("Eras Demi ITC", Font.PLAIN, 20));
    name.setForeground(Color.white);
    name.setSize(100, 20);
    name.setLocation(30, 100);
    p1.add(name);

    // TextfieldName details
    tName = new JTextField();
    tName.setFont(new Font("Eras Medium ITC", Font.PLAIN, 15));
    tName.setForeground(backgroundColor);
    tName.setBackground(Color.white);
    tName.setSize(400, 30);
    tName.setLocation(120, 95);
    p1.add(tName);

    // labelID details
    id = new JLabel("ID:");
    id.setFont(new Font("Eras Demi ITC", Font.PLAIN, 20));
    id.setForeground(Color.white);
    id.setSize(100, 20);
    id.setLocation(30, 200);
    p1.add(id);

    // TextfieldID details
    tID = new JTextField();
    tID.setFont(new Font("Eras Medium ITC", Font.PLAIN, 15));
    tID.setForeground(backgroundColor);
    tID.setBackground(Color.white);
    tID.setSize(400, 30);
    tID.setLocation(120, 200);
    p1.add(tID);

    // labelPhone details
    phone = new JLabel("Phone:");
    phone.setFont(new Font("Eras Demi ITC", Font.PLAIN, 20));
    phone.setForeground(Color.white);
    phone.setSize(100, 20);
    phone.setLocation(30, 300);
    p1.add(phone);

    // TextfieldPhone details
    tPhone = new JTextField();
    tPhone.setFont(new Font("Eras Medium ITC", Font.PLAIN, 15));
    tPhone.setForeground(backgroundColor);
    tPhone.setBackground(Color.white);
    tPhone.setSize(400, 30);
    tPhone.setLocation(120, 300);
    p1.add(tPhone);

    // labelEmail details
    email = new JLabel("Email:");
    email.setFont(new Font("Eras Demi ITC", Font.PLAIN, 20));
    email.setForeground(Color.white);
    email.setSize(100, 20);
    email.setLocation(30, 400);
    p1.add(email);

    // TextfieldEmail details
    tEmail = new JTextField();
    tEmail.setFont(new Font("Eras Medium ITC", Font.PLAIN, 15));
    tEmail.setForeground(backgroundColor);
    tEmail.setBackground(Color.white);
    tEmail.setSize(400, 30);
    tEmail.setLocation(120, 400);
    p1.add(tEmail);

    // Clear Button details,Button to clear data from textFields
    clearButton = new JButton("Clear");
    clearButton.setFont(new Font("Eras Demi ITC", Font.PLAIN, 15));
    clearButton.setSize(100, 30);
    clearButton.setLocation(300, 500);
    clearButton.setBackground(buttoncolor);
    clearButton.setForeground(Color.white);
    clearButton.addActionListener(this);
    p1.add(clearButton);

    // save Button details to, Button save data textFields
    saveButton = new JButton("Save");
    saveButton.setFont(new Font("Eras Demi ITC", Font.PLAIN, 15));
    saveButton.setSize(100, 30);
    saveButton.setLocation(420, 500);
    saveButton.setBackground(buttoncolor);
    saveButton.setForeground(Color.white);
    saveButton.addActionListener(this);
    p1.add(saveButton);
  }

  public Teacher getInsertedTeacher() {
    return teacher;
  }

  @Override
  public void actionPerformed(ActionEvent e) {

    if (e.getSource() == clearButton) {
      tName.setText("");
      tID.setText("");
      tPhone.setText("");
      tEmail.setText("");
    } else if (e.getSource() == saveButton) {

      if (tName.getText().isBlank() || tID.getText().isBlank()) {
        JOptionPane.showMessageDialog(
            this, "Name and ID are required", "Missing fields", JOptionPane.ERROR_MESSAGE);
      } else {

        if (specChars.matcher(tName.getText()).find()) {
          JOptionPane.showMessageDialog(
              this, "Name must contain letters only", "Invalid field", JOptionPane.ERROR_MESSAGE);
          return;
        }

        if (!tID.getText().isBlank()) {
          try {
            Integer.parseInt(tID.getText());
          } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                this, "ID must only contain numbers", "Invalid field", JOptionPane.ERROR_MESSAGE);
            return;
          }

          if (checkExistsData(tID.getText(), tPhone.getText(), tEmail.getText()) == 1) {
            JOptionPane.showMessageDialog(
                this,
                "A teacher with the same ID is already added",
                "Invalid field",
                JOptionPane.ERROR_MESSAGE);
            return;
          }
        }

        if (!tPhone.getText().isBlank()) {
          try {
            Integer.parseInt(tPhone.getText());
          } catch (NumberFormatException ex2) {
            JOptionPane.showMessageDialog(
                this,
                "Phone must only contain numbers",
                "Invalid field",
                JOptionPane.ERROR_MESSAGE);
            return;
          }

          if (checkExistsData(tID.getText(), tPhone.getText(), tEmail.getText()) == 2) {
            JOptionPane.showMessageDialog(
                this, "Duplicate phone number", "Invalid field", JOptionPane.ERROR_MESSAGE);
            return;
          }
        }

        if (!tEmail.getText().isBlank()) {
          if (emailChars.matcher(tEmail.getText()).find()) {
            JOptionPane.showMessageDialog(
                this,
                "Email Can't Accept special Chararcters except @ , . , - , _",
                "Invalid field",
                JOptionPane.ERROR_MESSAGE);
            return;
          }

          if (checkExistsData(tID.getText(), tPhone.getText(), tEmail.getText()) == 3) {
            JOptionPane.showMessageDialog(
                this,
                "A teacher with the same email is already present",
                "Invalid field",
                JOptionPane.ERROR_MESSAGE);
            return;
          }
        }

        teacherName = tName.getText();
        teacherID = Integer.parseInt(tID.getText());

        t1 = new Teacher.Builder().setName(teacherName).setId(teacherID);

        if (tPhone.getText().isBlank() == false) {
          teacherPhone = tPhone.getText();
          t1.setPhone(teacherPhone);
        }

        if (tEmail.getText().isBlank() == false) {
          teacherEmail = tEmail.getText();
          t1.setEmail(teacherEmail);
        }

        teacher = t1.build();
        dispose();
      }
    }
  }

  public int checkExistsData(String ID, String phone, String Email) {
    for (int i = 0; i < addedTeachers.size(); i++) {
      if (addedTeachers.get(i).getId() == Integer.parseInt(ID)) {
        return 1;
      }

      if (addedTeachers.get(i).isPhoneEmpty() == false) {

        if (phone.equals(addedTeachers.get(i).getPhone())) {
          return 2;
        }
      }

      if (addedTeachers.get(i).isEmailEmpty() == false) {

        if (Email.equals(addedTeachers.get(i).getEmail())) {
          return 3;
        }
      }
    }
    return 4;
  }
}
