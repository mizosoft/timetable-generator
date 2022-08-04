package com.github.mizosoft.timetablegenerator;

import com.github.mizosoft.timetablegenerator.Models.Group;
import com.github.mizosoft.timetablegenerator.Models.Lesson;
import com.github.mizosoft.timetablegenerator.Models.Period;
import com.github.mizosoft.timetablegenerator.Models.ProblemInstance;
import com.github.mizosoft.timetablegenerator.Models.Teacher;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public final class Samples {
  private Samples() {}

  private static void readRequirements(
      Document doc,
      Set<Lesson> lessons,
      Map<Lesson, Integer> weeklyOccurrences,
      Map<Lesson, Integer> maxDailyOccurrences,
      Map<Lesson, Integer> doubleLessons,
      Map<String, Teacher> teachers,
      Map<String, Group> groups) {
    var requirementsList = doc.getElementsByTagName("requirement");
    for (int i = 0; i < requirementsList.getLength(); i++) {
      var req = (Element) requirementsList.item(i);
      var teacher =
          teachers.computeIfAbsent(req.getAttribute("teacher"), id -> new Teacher(id, id));
      var group = groups.computeIfAbsent(req.getAttribute("class"), id -> new Group(id, id));
      var lesson = new Lesson(teacher, group);
      lessons.add(lesson);
      weeklyOccurrences.put(lesson, Integer.parseInt(req.getAttribute("lessons")));
      maxDailyOccurrences.put(lesson, Integer.parseInt(req.getAttribute("max_per_day")));
      doubleLessons.put(lesson, Integer.parseInt(req.getAttribute("double_lessons")));
    }
  }

  private static Map<Teacher, Set<Period>> readUnavailabilities(
      Document doc, HashMap<String, Teacher> teachers) {
    var unavailabilities = new LinkedHashMap<Teacher, Set<Period>>();
    var unavailabilityList = doc.getElementsByTagName("unavailability");
    for (int i = 0; i < unavailabilityList.getLength(); i++) {
      var unavail = (Element) unavailabilityList.item(i);
      var teacher = teachers.get(unavail.getAttribute("teacher"));
      int day = Integer.parseInt(unavail.getAttribute("day"));
      int slot = Integer.parseInt(unavail.getAttribute("period"));
      unavailabilities.computeIfAbsent(teacher, __ -> new HashSet<>()).add(new Period(day, slot));
    }
    return unavailabilities;
  }

  private static int readCount(Document doc, String name) {
    return Integer.parseInt(((Element) doc.getElementsByTagName(name).item(0)).getAttribute("to"))
        - Integer.parseInt(((Element) doc.getElementsByTagName(name).item(0)).getAttribute("from"))
        + 1;
  }

  static ProblemInstance readInstance(String filename) {
    try {
      var documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      var doc = documentBuilder.parse(new File("samples/instances/" + filename));

      int groupCount = readCount(doc, "classes");
      int teacherCount = readCount(doc, "teachers");
      int dayCount = readCount(doc, "days");
      int slotCount = readCount(doc, "periods");

      var lessons = new LinkedHashSet<Lesson>();
      var weeklyOccurrences = new LinkedHashMap<Lesson, Integer>();
      var maxDailyOccurrences = new LinkedHashMap<Lesson, Integer>();
      var teachers = new LinkedHashMap<String, Teacher>(teacherCount);
      var doubleLessons = new LinkedHashMap<Lesson, Integer>();
      var groups = new LinkedHashMap<String, Group>(groupCount);
      readRequirements(
          doc, lessons, weeklyOccurrences, maxDailyOccurrences, doubleLessons, teachers, groups);

      var unavailabilities = readUnavailabilities(doc, teachers);

      return new ProblemInstance(
          dayCount,
          slotCount,
          teacherCount,
          groupCount,
          lessons.stream().map(Lesson::teacher).collect(Collectors.toSet()),
          lessons.stream().map(Lesson::group).collect(Collectors.toSet()),
          lessons,
          weeklyOccurrences,
          maxDailyOccurrences,
          unavailabilities,
          doubleLessons);
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  static ProblemInstance readInstance() {
    return readInstance("NE-CESVP-2011-M-D.xml");
  }
}
