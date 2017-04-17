/**
  * Copyright (c) 2014-2015 Uncharted Software Inc. All rights reserved.
  *
  * Property of Uncharted(tm), formerly Oculus Info Inc.
  * http://uncharted.software/
  *
  * This software is the confidential and proprietary information of
  * Uncharted Software Inc. ("Confidential Information"). You shall not
  * disclose such Confidential Information and shall use it only in
  * accordance with the terms of the license agreement you entered into
  * with Uncharted Software Inc.
  */
package software.uncharted.sparkpipe.ops.text.analytics

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row}
import software.uncharted.sparkpipe.ops.core.rdd
import software.uncharted.sparkpipe.ops.text.analytics
import software.uncharted.sparkpipe.ops.text.transformations
import software.uncharted.sparkpipe.spark.SparkFunSpec

class LDAOperationTests extends SparkFunSpec {
  val defaultDictionaryConfig = DictionaryConfig(caseSensitive = true, None, None, None, None, None)

  describe("Word determination") {
    it("should break a text apart into words properly, including dealing with contractions") {
      val words =
        """She said, 'The quick brown fox doesn't ever jump over the lazy
          |dog.  I say it won't, it doesn't, and it didn't, and I'm sticking
          |to that!""".split(transformations.notWord).toList
      assert(List(
        "She", "said", "The", "quick", "brown", "fox", "doesn't", "ever", "jump", "over",
        "the", "lazy", "dog", "I", "say", "it", "won't", "it", "doesn't", "and", "it",
        "didn't", "and", "I'm", "sticking", "to", "that") === words)
    }
  }

  describe("#lda") {
    it("should perform LDA on a simple set of texts") {
      val texts = Seq(
        "aaa bbb ccc ddd",
        "aaa bbb eee fff",
        "aaa bbb ggg hhh",
        "ccc ddd eee fff",
        "ccc ddd ggg hhh",
        "eee fff ggg hhh"
      )
      val rddData = sc.parallelize(texts.zipWithIndex).map { case (text, index) =>
        new LDATestData(index, text)
      }
      val data = rdd.toDF(sparkSession)(rddData)
      val rawResults = analytics.textLDA("index", "text", defaultDictionaryConfig, LDAConfig(4, 2, 4, None, None, "", "", ""))(data)

      // Make sure we get the right number of results
      val results = interpretResults(rawResults).collect
      results.foreach { case (index, topics) =>
        assert(topics.length === 4)
        topics.foreach { case (wordScores, topicScore) =>
          wordScores.length === 2
        }
      }
    }

    it("should perform LDA on a complex set of texts") {
      // 10 questions from StackOverflow on recursion and java
      val javaRecursionTexts = Seq(
        "How do I recursively list all files under a directory in Java? Does the framework provide any utility? I saw a lot of hacky implementations. But none from the framework or nio",
        "I have been working on a Java project for a class for a while now. It is an implementation of a linked list (here called AddressList, containing simple nodes called ListNode). The catch is that everything would have to be done with recursive algorithms. I was able to do everything fine sans one method: public AddressList reverse()",
        "Some time ago, I've blogged about a Java 8 functional way of calculating fibonacci numbers recursively, with a ConcurrentHashMap cache and the new, useful computeIfAbsent() method:",
        "For the purposes of estimating the maximum call depth a recursive method may achieve with a given amount of memory, what is the (approximate) formula for calculating the memory used before a stack overflow error is likely to occur?",
        "I'm currently just working my way through some recursion problems, and I am currently stuck on one. The problem is to recursively insert spaces into a string, into every single possible location, such that the output looks something like:",
        "Here is some Java code to reverse a string recursively. Could someone provide an explanation of how it works?",
        "I would like to get all combination of a number without any repetition. Like 0.1.2, 0.2.1, 1.2.0, 1.0.2, 2.0.1, 2.1.0. I tried to find an easy scheme, but couldn't. I drew a graph/tree for it and this screams to use recursion. But I would like to do this without recursion, if this is possible.",
        "I decided to try a few experiments to see what I could discover about the size of stack frames, and how far through the stack the currently executing code was. There are two interesting questions we might investigate here: How many levels deep into the stack is the current code? How many levels of recursion can the current method reach before it hits a StackOverflowError?",
        "The recursion is sort of a 'divide and conquer' style, it splits up while getting smaller (Tree data structure), and I want it to break completely if a violation is found, meaning break all the recursive paths, and return true. Is this possible?",
        "I've been working on the 8 queens problem but I got stuck. I don't want code. I would love guidance and directions in order to understand how to solve this problem myself using backtracking recursion. The program should enumerate all solutions to the N-queens problem by drawing the location of the queens in ASCII like the two solutions here."
      )
      // 10 question from StackOverflow on guitars
      val guitarTexts = Seq(
        "Whats a good digital signal processing algorithm that is good on guitar chords? Since Fast Fourier Transform I think only is accurate on single notes played on the guitar but not notes that are played simultaenously (i.e. chords).",
        "I have a site that shows guitar chords/tabs in text format. Here's what I am currently displaying: I learn that GD can create a dynamic image for this. But I am new at PHP and I have no idea what to do. Is it simple to create such thing in PHP to display an image?",
        "I want to build a guitar tuner app for Iphone. My goal is to find the fundamental frequency of sound generated by a guitar string. I have used bits of code from aurioTouch sample provided by Apple to calculate frequency spectrum and I find the frequency with the highest amplitude . It works fine for pure sounds (the ones that have only one frequency) but for sounds from a guitar string it produces wrong results. I have read that this is because of the overtones generate by the guitar string that might have higher amplitudes than the fundamental one. How can I find the fundamental frequency so it works for guitar strings? Is there an open-source library in C/C++/Obj-C for sound analyzing (or signal processing)?",
        "I was wondering if anybody had heard of a library, preferably a .NET assembly, but Java will do as wel, that allows you to read the data in a Guitar Pro file (.gp3-gp4-gp5) I have this gigantor of a folder with about 50.000 song files, and would really love to write something that can actually archive all these files, for easier searching. And basic information like the tuning of the instruments in the song would be very useful parameters to retrieve from the file and add to the database.",
        "What i'm trying to achieve is playing a guitar chord from my python application. I know (or can calculate) the frequencies in the chord if needed. I'm thinking that even if I do the low level leg work of producing multiple sine waves at the right frequencies it wont sound right due to the envelope needing to be correct also, else it wont sound like a guitar but more of a hum.",
        "I know that the latest versions of Android (Honeycomb and ICS) have support for joysticks and gamepads. Guitar Hero (and Garage Band) controllers are essentially USB HID devices, right? So my question: Is that possible to receive data (button clicks) from the Guitar Hero (or Rock Band) controllers on Android device? Would the Android understand it as a gamepad input? P.S. all I need is to detect in my game the input from those five buttons on the plastic guitar fret.",
        "I'm trying to figure out a good way to be able to store plain text music lyrics with synchronized guitar chords. When displayed, I'd like to see the lyrics rendered double-spaced with the chords in the \"whitespace\" line above the corresponding words. This is for my own personal lyrics book so ultimately the rendering will be most commonly on dead trees vs a screen but this may change eventually. I figured I'd give Markdown a shot and was able to get the following format \"sort of\" working when combined with some CSS to have <a> tags render as quasi-superscripts. I kind of like how using the <a> tag worked out, because I can have reference links on the bottom of the file for any included chords:",
        "Hi I'm a noob in audio related coding and I'm working in a pitch tracking DLL that I will use to try to create a sort of open-source version of the video-game Rocksmith as a learning experience. So far I have managed to get the FFT to work so I can detect pitch frequency (Hz) then by using an algorithm and the table below I can manage to determine the octave (2th to 6th) and the note (C to B) for played note. The next step is to detect the string so I can determine the fret.",
        "I'm writing a quick front end to display guitar tablature. The front end is in Flash but I want to store the tab in some human-readable format. Anyone know of something that already exists? Any suggestions on how to go about it? One idea I got from reading some stackoverflow posts was to use a strict ASCII tab format like so: It has advantages. I can gain a lot of info from the structure (how many strings, their tunings, the relative placement of notes) but it is a bit verbose. I'm guessing the '-'s will compress away pretty well when sent over the wire.",
        "I'm currently working on making a game inspired by Guitar Hero and Frets on Fire, so far everything's been going well - I've written a script that can parse .chart files generated by the FeedBack Editor into usable data. My concern is how would I go about to make sure the timing is correct_(I'm gonna have to convert these beat values into ms instead)_? The files I'm parsing hold values such as these; Where the first integer is at what beat the note should occur, N is whether or not the note is a hammer-on, then the Fret ID_(Green or Red etc)_ and the length of the note, again in beats."
      )
      val rddData = sc.parallelize((javaRecursionTexts ++ guitarTexts).zipWithIndex).map{case (text, index) =>
          new LDATestData(index, text)
      }

      val data = rdd.toDF(sparkSession)(rddData)
      val rawResults = analytics.textLDA("index", "text", defaultDictionaryConfig, LDAConfig(2, 20, 2, None, None, "", "", ""))(data)


      // Make sure we get the right number of results
      val results = interpretResults(rawResults).collect
      results.foreach { case (index, topics) =>
        assert(topics.length === 2)
        topics.foreach { case (wordScores, topicScore) =>
          wordScores.length === 20
        }
      }
    }
  }

  def interpretResults(output: DataFrame): RDD[(Long, (List[(List[(String, Double)], Double)]))] = {
    output.select("index", "topics").rdd.map { row =>
      val index = row.getLong(0)
      val topics = row.get(1).asInstanceOf[Seq[Row]].map { topicRow =>
        val wordScores = topicRow.get(0).asInstanceOf[Seq[Row]].map { wordScoreRaw =>
          val word = wordScoreRaw.getString(0)
          val score = wordScoreRaw.getDouble(1)
          (word, score)
        }.toList
        val topicScore = topicRow.getDouble(1)
        (wordScores, topicScore)
      }.toList
      (index, topics)
    }
  }
}

case class LDATestData (index: Long, text: String)
