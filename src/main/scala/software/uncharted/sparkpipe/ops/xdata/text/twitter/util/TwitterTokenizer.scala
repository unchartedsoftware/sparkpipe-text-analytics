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
package software.uncharted.sparkpipe.ops.xdata.text.twitter.util

/**
  * Container for utility functions used to clean tweet text.
  */
// scalastyle:off line.size.limit multiple.string.literals
object TwitterTokenizer extends Serializable {
  // Emoji
  // ---------------------------------------------------------------------------------------------------
  val emoji = List(
    ("[😀|🤣|😃|😄|😅|😆|😊|☺|🙂|?]+", " <HAPPY_FACE> "),
    ("[😋|😛|😜|😝]+", " <LOL> "),
    ("[😁|😺]+", " <GRINNING_FACE> "),
    ("[😂]+", " <LAUGHING_CYING_FACE> "),
    ("[😉]+", " <WINK> "),
    ("[😎]+", " <COOL> "),
    ("[🤓]+", " <NERD> "),
    ("[👨‍❤️‍👨|👨‍❤️‍💋‍👨|👩‍❤️‍👨|👩‍❤️‍👩|👩‍❤️‍💋‍👨|👩‍❤️‍💋‍👩|💏|💑]+", " <RELATIONSHIP> "),
    ("[😍|😻]+", " <HEART_FACE> "),
    ("[😘|😗|😙|😚|😽]+", " <KISS_FACE> "),
    ("[💘|❤|💓|💕|💖|💗|💙|💚|💛|💜|💝|💞|💟|❣|💌]+", " <HEART> "),
    ("[👄|💋]+", " <LIPS> "),
    ("[🤗]+", " <HUG_FACE> "),
    ("[🤤]+", " <DROOLING> "),
    ("[🙃]+", " <UPSIDEDOWN_SMILE> "),
    ("[🤑]+", " <MONEY_FACE> "),
    ("[😐|😑|😶]+", " <NEUTRAL> "),
    ("[🤔]+", " <THINKING> "),
    ("[😒]+", " <UNAMUSED> "),
    ("[🙄]+", " <ROLLING_EYES> "),
    ("[😏|😼]+", " <SMIRK> "),
    ("[😣]+", " <PERSEVERING> "),
    ("[😥|😞]+", " <SAD_FACE> "),
    ("[😮|😯]+", " <SURPRISED> "),
    ("[😪|😫|😴|💤]+", " <TIRED> "),
    ("[😌]+", " <RELIEVED> "),
    ("[🤐]+", " <ZIPPER_FACE> "),
    ("[😓]+", " <SAD_SWEAT> "),
    ("[😔]+", " <PENSIVE> "),
    ("[😕]+", " <CONFUSED> "),
    ("[😲]+", " <ASTONISHED> "),
    ("[😖]+", " <QUIVERING_FACE> "),
    ("[😳]+", " <EMBARRASSED> "),
    ("[😟|🙀]+", " <WORRIED> "),
    ("[😢|😭|😿]+", " <CRY_FACE> "),
    ("[☹|🙁|😦|😧|😨]+", " <FROWNING_FACE> "),
    ("[😩]+", " <WAILING> "),
    ("[😰]+", " <COLD_SWEAT> "),
    ("[😬|😱]+", " <FEAR_FACE> "),
    ("[😤]+", " <FRUSTRATION> "),
    ("[😵|😡|😠|😾]+", " <ANGRY_FACE> "),
    ("[💢]+", " <ANGER> "),
    ("[💣]+", " <BOMB> "),
    ("[💥]+", " <BANG_EMOJI> "),
    ("[🖕]+", " <RUDE_FINGER> "),
    ("[🤙]+", " <SHAKA_HAND> "),
    ("[🖐|✋]+", " <RAISED_HAND> "),
    ("[👌]+", " <OK_HAND> "),
    ("[👍]+", " <THUMBS_UP> "),
    ("[👎]+", " <THUMBS_DOWN> "),
    ("[✊]+", " <RAISED_FIST> "),
    ("[👊|🤛|🤜]+", " <FIST_BUMP> "),
    ("[👏]+", " <CLAPPING_HANDS> "),
    ("[🙌]+", " <PRAISE_HANDS> "),
    ("[🙏]+", " <PRAYER_HANDS> "),
    ("[🤝]+", " <HANDSHAKE> "),
    ("[🖤]+", " <BLACK_HEART> "),
    ("[💔]+", " <BROKEN_HEART> ")
  )

  // ---------------------------------------------------------------------------------------------------
  val unicodeOutliers    = "[^\u0000-\uFFFF]"   // all unicode code points outside the basic lingual plane
  val notNeededOther    = "[\\p{InIPA_Extensions}|\\p{InKhmer_Symbols}|\\p{InPhonetic_Extensions}|\\p{InArrows}|\\p{InMathematical_Operators}|\\p{InMiscellaneous_Technical}|\\p{InControl_Pictures}|\\p{InOptical_Character_Recognition}|\\p{InEnclosed_Alphanumerics}|\\p{InBox_Drawing}|\\p{InBlock_Elements}|\\p{InGeometric_Shapes}|\\p{InMiscellaneous_Symbols}|\\p{InDingbats}|\\p{InBraille_Patterns}|\\p{InSupplemental_Mathematical_Operators}|\\p{InMiscellaneous_Symbols_and_Arrows}|\\p{InCJK_Symbols_and_Punctuation}|\\p{InPrivate_Use_Area}|\\p{InVariation_Selectors}|\\p{InCombining_Half_Marks}|\\p{InSpecials}]+"
  val notNeededLangs    = "[\\p{IsBengali}|\\p{IsBopomofo}|\\p{IsBuhid}|\\p{IsCanadian_Aboriginal}|\\p{IsCherokee}|\\p{IsDevanagari}|\\p{IsEthiopic}|\\p{IsGeorgian}|\\p{IsGreek}|\\p{IsGujarati}|\\p{IsGurmukhi}|\\p{IsMalayalam}|\\p{IsMongolian}|\\p{IsMyanmar}|\\p{IsOgham}|\\p{IsOriya}|\\p{IsRunic}|\\p{IsSinhala}|\\p{IsSyriac}|\\p{IsTagalog}|\\p{IsTagbanwa}|\\p{IsTamil}|\\p{IsTelugu}|\\p{IsThaana}|\\p{IsThai}]|[\\p{IsHanunoo}|\\p{IsKannada}|\\p{IsKhmer}|\\p{IsLao}|\\p{IsLimbu}|\\p{IsYi}|\\p{IsHiragana}|\\p{IsKatakana}|\\p{IsHangul}|\\p{IsHan}]+"
  // HTML_CHAR_CODES     = "\\b&?(quot|amp|lt|gt|nbsp|iexcl|cent|pound|curren|brvbar|sect|uml|copy|ordf|laquo|not|shy|reg|macr|deg|plusmn|sup2|sup3|acute|para|middot|cedil|sup1|ordm|raquo|frac14|frac12|frac34|iquest|eth|eth|aelig|aelig|oelig|oelig|aring|oslash|ccedil|ccedil|szlig|ntilde|ntilde);?\\b"
  val htmlCharCodes     = "\\b&?(quot|amp|lt|gt|nbsp|iexcl|cent|pound|curren|brvbar|sect|uml|copy|ordf|laquo|not|shy|reg|macr|deg|plusmn|sup2|sup3|acute|para|middot|cedil|sup1|ordm|raquo|frac14|frac12|frac34|iquest|eth|eth|aelig|aelig|oelig|oelig|aring|oslash|ccedil|ccedil|szlig|ntilde|ntilde);?\\b"

  // ---------------------------------------------------------------------------------------------------
  val retweetPat       = "\\b(rt)|(RT)|(retweeted)\\b"
  val urlRegex1         = "https?:\\/\\/\\S+\\b|www\\.(\\w+\\.)+\\S*"
  val urlRegex2         = "https?[\\.\\w\\-\\_\\/]+\\b"
  val slashRegex       = "/"
  val userRegex        = "@[\\w0-9_]+[:\\.,]?"
  val hyphenRegex      = "[-\\~\\֊\\־\\᐀\\᠆\\‐\\‑\\‒\\–\\—\\―\\⁓\\⁻\\₋\\−\\⸗\\⸺\\⸻\\〜\\〰\\゠\\︱\\︲\\﹘\\﹣\\－]+"
  val quoteRegex       = "\\u0022|\\u0027|\\u0060|\\u00B4|\\u2018|\\u2019|\\u201C|\\u201D"
  val bracketRegex     = "<(\\p{Lower}+)>"
  val cutoffRegex      = "\\b\\w+…"   // n.b. retweeted text if frequently cutoff mid-word. e.g. =>  comunicación... htt…
  val unicodeSpaces    = "[\\u0009-\\u000d|\\u0020|\\u0085|\\u00a0|\\u1680|\\u180E|\\u2000-\\u200a|\\u2028|\\u2029|\\u202F|\\u205F|\\u3000]+"

  // ---------------------------------------------------------------------------------------------------
  val ellipsis = "[\\u2026]"
  val loneDashPat = "\\s?\\-\\s|\\s\\-\\s?"
  val punctPat =  "[\\p{P}&&[^#-]]"
  val symbolPat = "[\\p{S}--[<>]]"

  val loneNumPat = "(^|\\s)\\p{Digit}+($|\\s)"
  val loneCharPat = "\\b\\p{L}\\b"
  val ctrlCharPat = "\\p{C}"
  val spacePat = "[\\p{Z}\\s]+"

  // ---------------------------------------------------------------------------------------------------
  // emoticons
  val emoticons = List(
    ("(?i)\\b([8:=;]['`\\-]?[)d]+|[(d]+['`\\-]?[8:=;]|^-^)\\b", " <SMILE> "),
    ("(?i)\\b([8:=;]['`\\-]?[Þþp]+|x-?p|>:p)\\b", " <LOL> "),
    ("\\b([8:=;]['`\\-]?\\(+|\\)+['`\\-]?[8:=;])\\b", " <SAD_FACE> "),
    ("\\b(;_;|x_x|u_u|t_+t|>_?<'?|>:/|>:\\(|=\\(|;^;)\\b", " <SAD_FACE> "),
    ("\\b(:[-^]?\\*)\\b", " <KISS> "),
    ("(?i)\\b([;\\*]\\-?[\\)\\]]|;D|\\;\\^\\)?|:-,)\\b", " <WINK> "),
    ("\\b([8:=;]['`\\-]?[\\/|l*])\\b", " <NEUTRAL> "),
    ("\\b(\\*-+\\*|:\\/)\\b", " <NEUTRAL> "),
    ("(?i)\\b([o0]_[o0]|>:O|:-?O)\\b", " <SURPRISED> "),
    ("\\b<3\\b", " <HEART> ")
  )
  val numberRegex        = "(?i)(\\b(no|[-+])?[.\\d]*[\\d]+[:,.\\d]*)(st|nd|rd|th|am|pm)?\\b" // lone numbers (not within a word), optionally followed by st, th, rd, am, pm
  val repeatedPunctRegex = "([!?.])+"
  val elongatedFinalRegex= "\\b(\\S*?)(.)\\2+\\b"

  // ---------------------------------------------------------------------------------------------------
  // remove lone dash
  val loneHyphen = "\\s?\\-\\s|\\s\\-\\s?"
  // remove punctuation & symbols except for hypens, hashes and <TAG> brackets
  //  val punct       = "[\\p{P}&&[^<>#-]]"
  // remove lone digits
  val loneDigits  = "(^|\\s)\\p{Digit}+($|\\s)"
  // remove single characters
  val singleChar  = "\\b\\p{L}\\b"
  // remove control characters
  val controlChar = "\\p{C}"
  // remove extra spaces, control characters
  val spaces      = "[\\p{Z}\\s]+"


  // detect acronyms
  val acronymPattern = "(?<=\\s|^)((?:[a-zA-Z]\\.){2,})(?=[[:punct:]]?(?:\\s|$))".r
  val sentenceFinalPeriod = "\\.([A-z]{2})".r   // for use in detecting non-acronym sentences that end.And the next begins without a space
  val acronymPeriod =  "((?<=[A-z])\\.)+".r

  // ==============================================================================================================
  /**
    * Find meaningful emoji patterns, ignoring repeated patterns, substitute emoji
    * for a text identifier  <TAG>. Replace all emoji not given by the regex
    * patterns by an empty string (i.e. delete them).
    */
  def normalizeEmoji(text: String): String = {
    var txt = text
    emoji.foreach(em => txt = em._1.r.replaceAllIn(txt, em._2))
    txt = unicodeOutliers.r.replaceAllIn(txt, "")
    txt.trim
  }

  /**
    * Replace meaningful emoji patterns, ignoring repeated patterns, with a space.
    * Replace all emoji not given by the regex patterns by an empty string (i.e. delete them).
    */
  def replaceEmoji(text: String): String = {
    var txt = text
    emoji.foreach(em => txt = em._1.r.replaceAllIn(txt, " "))
    txt = unicodeOutliers.r.replaceAllIn(txt, "")
    txt.trim
  }

  /**
    *  Convert emoticons <TAGS>.
    */
  def normalizeEmoticons(text: String): String = {
    var txt = text
    emoticons.foreach(em => txt = em._1.r.replaceAllIn(txt, em._2))
    txt.trim
  }

  /**
    *  Remove emoticons.
    */
  def replaceEmoticons(text: String): String = {
    var txt = text
    emoticons.foreach(em => txt = em._1.r.replaceAllIn(txt, " "))
    txt.trim
  }

  /**
    *  Convert emoticons, emoji and patterns to <TAGS>
    */
  // scalastyle:off return
  def normalize(text: String, removeEmotiji:Boolean=true): String = {
    val otherlang = notNeededLangs.r.findFirstIn(text).isDefined
    if (otherlang) {
      return ""
    } else {
      var txt = text
      if (removeEmotiji) {
        txt = replaceEmoji(text)
        txt = replaceEmoticons(txt)
      }
      else{
        txt = normalizeEmoji(text)
        txt = normalizeEmoticons(txt)

      }
      txt = notNeededOther.r.replaceAllIn(txt, "")
      txt = htmlCharCodes.r.replaceAllIn(txt, " ")
      txt = cutoffRegex.r.replaceAllIn(txt, "")
      txt = urlRegex1.r.replaceAllIn(txt, " ")
      txt = urlRegex2.r.replaceAllIn(txt, " ")
      txt = slashRegex.r.replaceAllIn(txt, " ")
      txt = userRegex.r.replaceAllIn(txt, " ")
      txt = numberRegex.r.replaceAllIn(txt, " ")
      txt = repeatedPunctRegex.r.replaceAllIn(txt, "$1")
      txt = elongatedFinalRegex.r.replaceAllIn(txt, "$1$2$2")
      txt = quoteRegex.r.replaceAllIn(txt, "")
      txt = bracketRegex.r.replaceAllIn(txt, "$1")
      txt = retweetPat.r.replaceAllIn(txt, " ")
      txt = ellipsis.r.replaceAllIn(txt, " ")
      txt = hyphenRegex.r.replaceAllIn(txt, "-")
      return txt.trim
    }
  }
  // scalastyle:on return

  /**
    *  Remove symbols and punctuation from a string of text
    */
  def clean(text: String) : String = {
    // normalize acronyms
    val t1 = sentenceFinalPeriod.replaceAllIn(text, " $1")
    val t2 = acronymPeriod.replaceAllIn(t1, "")
    // remove lone dash
    val t3 = loneDashPat.r.replaceAllIn(t2, " ")
    // remove punctuation & symbols except for hypens and <TAG> brackets
    val t4 = punctPat.r.replaceAllIn(t3, " ")
    val t5 = symbolPat.r.replaceAllIn(t4, " ")
    // remove lone digits
    val t6 = loneNumPat.r.replaceAllIn(t5, " ")
    // remove control characters
    val t7 = ctrlCharPat.r.replaceAllIn(t6, "")
    // remove extra spaces, control characters
    val t8 = spacePat.r.replaceAllIn(t7, " ")
    // remove multiple spaces
    t8.trim
  }

  /**
    * Wrapper function to normalize then clean tweet.
    * @param text Tweet text.
    * @return Normalized & cleaned tweet.
    */
  def normclean(text: String) : String = {
    val norm_ = normalize(text)
    val clean_ = clean(norm_)
    clean_
  }

  /**
    *  Tokenize an input text by splitting on white space
    */
  def tokenizeCleanText(text: String) : Array[String] = {
    text.split("\\p{Z}+")
  }

  /**
    *  Convenience method to run normalize, clean and tokenize
    */
  def tokenize(text: String) : Array[String] = {
    val norm_ = normalize(text)
    val clean_ = clean(norm_)
    val tokens = tokenizeCleanText(clean_)
    tokens
  }

}
// scalastyle:on line.size.limit multiple.string.literals
