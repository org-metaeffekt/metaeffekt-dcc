/**
 * Copyright 2009-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.metaeffekt.dcc.commons.ant.wrapper;

import org.metaeffekt.core.commons.annotation.Public;

@Public
public class StringBlockFormatter {
    
    /**
     * Convenience method. Delegates to the formatString method including
     * the maximum line length parameter.
     * 
     * @param string The string to insert line breaks in.
     * @param linePrefix The prefix that should be inserted before every line.
     * @param lineSuffix The suffix that should be appended to every line.
     * @return The formatted string.
     */
    public static String formatString(String string, String linePrefix, String lineSuffix) {
        return formatString(string, linePrefix, lineSuffix, 80);
    }

    /**
     * Formats a string to a maximum line length. This is mainly
     * used to format javadoc in the generator templates.
     * 
     * @param string The string to insert line breaks in.
     * @param linePrefix The prefix that should be inserted before every line.
     * @param lineSuffix The suffix that should be appended to every line.
     * @param maxLineLength The maximum length of a line.
     * @return The formatted string.
     */
    public static String formatString(String string, String linePrefix, String lineSuffix,
            Integer maxLineLength) {
        if (string == null) {
            return "";
        }
        string = cropHtmlDecoration(string.trim());
        String[] lines = string.split("\n");
        
        StringBuilder stringBuilder = new StringBuilder();
        
        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String[] words = lines[lineIndex].split(" ");
    
            int i = 0;
            while (i < words.length) {
                StringBuilder lineBuilder = new StringBuilder();
                boolean isFirstAppend = true;
                stringBuilder.append(linePrefix);
                while (isFirstAppend || 
                        (lineBuilder.length() + words[i].length() + 1 < maxLineLength)) {
                    if (!isFirstAppend) {
                        lineBuilder.append(' ');
                    }
                    lineBuilder.append(words[i]);
                    isFirstAppend = false;
    
                    i++;
    
                    if (i == words.length) {
                        break;
                    }
                }
    
                stringBuilder.append(lineBuilder);
                stringBuilder.append(lineSuffix);
            }
        }

        return stringBuilder.toString();
    }
    
    /**
     * Crops the HTML decoration of a documentation text.
     * If in MagicDraw the comment of a model element is entered in HTML style, the documentation
     * will be decorated with the following HTMl tags:
     * <pre>
     *      &lt;html&gt
     *          &lt;head&gt
     *          &lt;/head&gt
     *          &lt;body&gt
     *          &lt;/body&gt
     *      &lt;/html&gt
     * </pre>
     * These are removed to return only the documentation information.
     * 
     * @param string the documentation string in HTML style
     * @return the documentation string without the described tags in it
     */
    private static String cropHtmlDecoration(String string) {
        string = cropTag(string, "html");
        string = cropTag(string, "head");
        string = cropTag(string, "body");
        return string;
    }

    /**
     * Crops the first occurrence of the tag with the given name from the HTML code in 
     * <code>string</code>.
     * 
     * @param string the string holding the HTML code
     * @param tagName the name of the tag which gets cropped
     * @return the cropped HTML code
     */
    private static String cropTag(String string, String tagName) {
        if (string.indexOf("<" + tagName + ">") != -1 
                && string.lastIndexOf("</" + tagName + ">") != -1) {
            string = string.substring(tagName.length() + 2, string.length()).trim();
            
            if (string.lastIndexOf("</" + tagName + ">") == 0) {
                string = string.substring(tagName.length() + 3, string.length()).trim();
            }
            else {
                string = string.substring(0, string.length() - (tagName.length() + 3)).trim();
            }
        }
        return string;
    }

}
