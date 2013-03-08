package com.pongr.jetray

import org.specs2.mutable._
import java.text.SimpleDateFormat

class RunSpec extends Specification {
  "The sent frequency calculations" should {
    "calculate postSendBySecond correctly" in {
      val f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS Z")
      def email(id: Long, d: String) = FirstEmail("1", id, "", "", "", "", f parse d, f parse d, f parse d)

      val e1 = email(1, "2011-09-02 11:46:23:123 -0500")
      val e2 = email(2, "2011-09-02 11:46:23:234 -0500")
      val e3 = email(3, "2011-09-02 11:46:23:345 -0500")

      val e4 = email(4, "2011-09-02 11:46:24:123 -0500")
      val e5 = email(5, "2011-09-02 11:46:24:234 -0500")

      val e6 = email(1, "2011-09-02 11:46:26:123 -0500")
      
      val run = Run("1", List((e1, None), (e2, None), (e3, None), (e4, None), (e5, None), (e6, None)))

      val expectedSeconds = Map(
        (f parse "2011-09-02 11:46:23:000 -0500") -> List(e1, e2, e3),
        (f parse "2011-09-02 11:46:24:000 -0500") -> List(e4, e5),
        (f parse "2011-09-02 11:46:26:000 -0500") -> List(e6))

      run.postSendBySecond must_== expectedSeconds
      
      val expectedCounts = Map(
        (f parse "2011-09-02 11:46:23:000 -0500") -> 3,
        (f parse "2011-09-02 11:46:24:000 -0500") -> 2,
        (f parse "2011-09-02 11:46:26:000 -0500") -> 1)
        
      run.sentEmailFrequencies must_== expectedCounts
      
      run.averageOfSentEmailFrequencies must_== 2d
      run.averageSentEmailFrequency must_== 6d / ((e6.postSend.getTime - e1.postSend.getTime) / 1000d)
    }
  }
}