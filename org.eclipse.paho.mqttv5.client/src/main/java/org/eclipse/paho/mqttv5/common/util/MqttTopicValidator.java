package org.eclipse.paho.mqttv5.common.util;

import java.io.UnsupportedEncodingException;

public class MqttTopicValidator {

  /**
   * The forward slash (/) is used to separate each level within a topic tree and provide a hierarchical structure to
   * the topic space. The use of the topic level separator is significant when the two wildcard characters are
   * encountered in topics specified by subscribers.
   */
  public static final String TOPIC_LEVEL_SEPARATOR = "/";

  /**
   * Multi-level wildcard The number sign (#) is a wildcard character that matches any number of levels within a topic.
   */
  public static final String MULTI_LEVEL_WILDCARD = "#";

  /**
   * Single-level wildcard The plus sign (+) is a wildcard character that matches only one topic level.
   */
  public static final String SINGLE_LEVEL_WILDCARD = "+";

  /**
   * Multi-level wildcard pattern(/#)
   */
  public static final String MULTI_LEVEL_WILDCARD_PATTERN = TOPIC_LEVEL_SEPARATOR + MULTI_LEVEL_WILDCARD;

  /**
   * Topic wildcards (#+)
   */
  public static final String TOPIC_WILDCARDS = MULTI_LEVEL_WILDCARD + SINGLE_LEVEL_WILDCARD;

  // topic name and topic filter length range defined in the spec
  private static final int MIN_TOPIC_LEN = 1;
  private static final int MAX_TOPIC_LEN = 65535;
  private static final char NUL = '\u0000';

  /**
   * Validate the topic name or topic filter
   *
   * @param topicString
   *          topic name or filter
   * @param wildcardAllowed
   *          true if validate topic filter, false otherwise
   * @param sharedSubAllowed
   *          true if shared subscription is allowed, false otherwise
   * @throws IllegalArgumentException
   *           if the topic is invalid
   */
  public static void validate(String topicString, boolean wildcardAllowed, boolean sharedSubAllowed)
      throws IllegalArgumentException {
    int topicLen = 0;
    try {
      topicLen = topicString.getBytes("UTF-8").length;
    } catch (UnsupportedEncodingException e) {
      throw new IllegalStateException(e.getMessage());
    }

    // Spec: length check
    // - All Topic Names and Topic Filters MUST be at least one character
    // long
    // - Topic Names and Topic Filters are UTF-8 encoded strings, they MUST
    // NOT encode to more than 65535 bytes
    if (topicLen < MIN_TOPIC_LEN || topicLen > MAX_TOPIC_LEN) {
      throw new IllegalArgumentException(String.format("Invalid topic length, should be in range[%d, %d]!",
          new Object[] { Integer.valueOf(MIN_TOPIC_LEN), Integer.valueOf(MAX_TOPIC_LEN) }));
    }

    // *******************************************************************************
    // 1) This is a topic filter string that can contain wildcard characters
    // *******************************************************************************
    if (wildcardAllowed) {
      // Only # or +
      if (Strings.equalsAny(topicString, new String[] { MULTI_LEVEL_WILDCARD, SINGLE_LEVEL_WILDCARD })) {
        return;
      }

      // 1) Check multi-level wildcard
      // Rule:
      // The multi-level wildcard can be specified only on its own or next
      // to the topic level separator character.

      // - Can only contains one multi-level wildcard character
      // - The multi-level wildcard must be the last character used within
      // the topic tree
      if (Strings.countMatches(topicString, MULTI_LEVEL_WILDCARD) > 1
          || (topicString.contains(MULTI_LEVEL_WILDCARD) && !topicString.endsWith(MULTI_LEVEL_WILDCARD_PATTERN))) {
        throw new IllegalArgumentException("Invalid usage of multi-level wildcard in topic string: " + topicString);
      }

      // 2) Check single-level wildcard
      // Rule:
      // The single-level wildcard can be used at any level in the topic
      // tree, and in conjunction with the
      // multilevel wildcard. It must be used next to the topic level
      // separator, except when it is specified on
      // its own.
      validateSingleLevelWildcard(topicString);

      return;
    }

    // Validate Shared Subscriptions
    if (!sharedSubAllowed && topicString.startsWith("$share/")) {
      throw new IllegalArgumentException("Shared Subscriptions are not allowed.");
    }

    // *******************************************************************************
    // 2) This is a topic name string that MUST NOT contains any wildcard characters
    // *******************************************************************************
    if (Strings.containsAny(topicString, TOPIC_WILDCARDS)) {
      throw new IllegalArgumentException("The topic name MUST NOT contain any wildcard characters (#+)");
    }

  }

  private static void validateSingleLevelWildcard(String topicString) {
    char singleLevelWildcardChar = SINGLE_LEVEL_WILDCARD.charAt(0);
    char topicLevelSeparatorChar = TOPIC_LEVEL_SEPARATOR.charAt(0);

    char[] chars = topicString.toCharArray();
    int length = chars.length;
    char prev = NUL, next = NUL;
    for (int i = 0; i < length; i++) {
      prev = (i - 1 >= 0) ? chars[i - 1] : NUL;
      next = (i + 1 < length) ? chars[i + 1] : NUL;

      if (chars[i] == singleLevelWildcardChar) {
        // prev and next can be only '/' or none
        if (prev != topicLevelSeparatorChar && prev != NUL || next != topicLevelSeparatorChar && next != NUL) {
          throw new IllegalArgumentException(String
              .format("Invalid usage of single-level wildcard in topic string '%s'!", new Object[] { topicString }));

        }
      }
    }
  }

  /**
   * Check the supplied topic name and filter match
   *
   * @param topicFilter
   *          topic filter: wildcards allowed
   * @param topicName
   *          topic name: wildcards not allowed
   * @return true if the topic matches the filter
   * @throws IllegalArgumentException
   *           if the topic name or filter is invalid
   */
  public static boolean isMatched(String topicFilter, String topicName) throws IllegalArgumentException {
    int topicPos = 0;
    int filterPos = 0;
    int topicLen = topicName.length();
    int filterLen = topicFilter.length();

    MqttTopicValidator.validate(topicFilter, true, true);
    MqttTopicValidator.validate(topicName, false, true);

    if (topicFilter.equals(topicName)) {
      return true;
    }

    while (filterPos < filterLen && topicPos < topicLen) {
      if (topicFilter.charAt(filterPos) == '#') {
        /*
         * next 'if' will break when topicFilter = topic/# and topicName topic/A/, but they are matched
         */
        topicPos = topicLen;
        filterPos = filterLen;
        break;
      }
      if (topicName.charAt(topicPos) == '/' && topicFilter.charAt(filterPos) != '/')
        break;
      if (topicFilter.charAt(filterPos) != '+' && topicFilter.charAt(filterPos) != '#'
          && topicFilter.charAt(filterPos) != topicName.charAt(topicPos))
        break;
      if (topicFilter.charAt(filterPos) == '+') { // skip until we meet the next separator, or end of string
        int nextpos = topicPos + 1;
        while (nextpos < topicLen && topicName.charAt(nextpos) != '/')
          nextpos = ++topicPos + 1;
      } else if (topicFilter.charAt(filterPos) == '#')
        topicPos = topicLen - 1; // skip until end of string
      filterPos++;
      topicPos++;
    }

    if ((topicPos == topicLen) && (filterPos == filterLen)) {
      return true;
    } else {
      /*
       * https://github.com/eclipse/paho.mqtt.java/issues/418 Covers edge case to match sport/# to sport
       */
      if ((topicFilter.length() - filterPos > 0) && (topicPos == topicLen)) {
        if (topicName.charAt(topicPos - 1) == '/' && topicFilter.charAt(filterPos) == '#')
          return true;
        if (topicFilter.length() - filterPos > 1 && topicFilter.substring(filterPos, filterPos + 2).equals("/#")) {
          if ((topicFilter.length() - topicName.length()) == 2
              && topicFilter.substring(topicFilter.length() - 2, topicFilter.length()).equals("/#")) {
            return true;
          }
        }
      }
      /*
       * https://github.com/eclipse/paho.mqtt.java/issues/918
       * covers cases that include more then one wildcard
       * sport/+/tennis/#
       */
        String[] topicFilterParts = topicFilter.split(TOPIC_LEVEL_SEPARATOR);
        String[] topicParts = topicName.split(TOPIC_LEVEL_SEPARATOR);
        if(topicFilterParts.length -1 == topicParts.length &&
            topicFilterParts[topicFilterParts.length-1].equals( MULTI_LEVEL_WILDCARD)) {      
          for (int i = 0; i<topicParts.length;i++) {
            if(!topicParts[i].equals(topicFilterParts[i]) && !topicFilterParts[i].equals(SINGLE_LEVEL_WILDCARD)) {
              return false;
            }       
          }
          return true;
        }  
      
    }
    return false;
  }

}
