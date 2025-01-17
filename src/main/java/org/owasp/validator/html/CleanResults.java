/*
 * Copyright (c) 2007-2022, Arshan Dabirsiaghi, Jason Li
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. Neither the name of OWASP nor the names of its
 * contributors may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.owasp.validator.html;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.w3c.dom.DocumentFragment;

/**
 * This class contains the results of a scan. It primarily provides access to the clean sanitized
 * HTML, per the AntiSamy sanitization policy applied. It also provides access to some utility
 * information, like possible error messages and error message counts.
 *
 * <p>WARNING: The ONLY output from the class you can completely rely on is the CleanResults output.
 * As stated in the project <a
 * href="https://github.com/nahsra/antisamy/blob/main/README.md">README</a> file, neither the {@code
 * getErrorMessages()} nor the {@code getNumberOfErrors()} methods subtly answer the question "is
 * this safe input?" in the affirmative if it returns an empty list. You must always use the
 * sanitized 'clean' input and there is no way to be sure the input passed in had no attacks.
 *
 * <p>The serialization and deserialization process that is critical to the effectiveness of the
 * sanitizer is purposefully lossy and will filter out attacks via a number of attack vectors.
 * Unfortunately, one of the tradeoffs of this strategy is that AntiSamy doesn't always know in
 * retrospect that an attack was seen. Thus, the getErrorMessages() API is there to help users
 * understand whether their well-intentioned input meets the requirements of the system, not help a
 * developer detect if an attack was present.
 *
 * <p>The list of error messages (available via {@code getErrorMessages()}) will let the user know
 * what, if any HTML errors existed, and what, when possible, any security or validation-related
 * errors that were detected, and what was done about them.
 *
 * <p><b>WARNING</b>: As just stated, the <i>absence</i> of error messages does NOT mean there were
 * no attacks in the input that were sanitized out. You CANNOT rely on the {@code
 * getErrorMessages()} or {@code getNumberOfErrors()} methods to tell you if the input was
 * dangerous. You MUST use the output of {@code getCleanHTML()} to ensure your output is safe.
 *
 * @author Arshan Dabirsiaghi
 */
public class CleanResults {

  private List<String> errorMessages;
  private Callable<String> cleanHTML;
  private long startOfScan; // Time the scan started in milliseconds since epoch.
  private long elapsedScan; // Elapsed time for the scan, in milliseconds

  /*
   * A DOM object version of the clean HTML String. May be null even if clean HTML is set.
   */
  private DocumentFragment cleanXMLDocumentFragment;

  /*
   * Default constructor. Can be extended.
   */
  public CleanResults() {
    this.errorMessages = new ArrayList<String>();
  }

  /**
   * Create a clean set of results.
   *
   * @param startOfScan - The time when the scan started.
   * @param cleanHTML - The resulting clean HTML produced per the AntiSamy policy.
   * @param XMLDocumentFragment - The XML Document fragment version of the clean HTML produced
   *     during the sanitization process.
   * @param errorMessages - Messages describing any errors that occurred during sanitization.
   */
  public CleanResults(
      long startOfScan,
      final String cleanHTML,
      DocumentFragment XMLDocumentFragment,
      List<String> errorMessages) {

    this(
        startOfScan,
        new Callable<String>() {
          public String call() throws Exception {
            return cleanHTML;
          }
        },
        XMLDocumentFragment,
        errorMessages);
  }

  /**
   * Create a clean set of results.
   *
   * @param startOfScan - The time when the scan started.
   * @param cleanHTML - The resulting clean HTML produced per the AntiSamy policy.
   * @param XMLDocumentFragment - The XML Document fragment version of the clean HTML produced
   *     during the sanitization process.
   * @param errorMessages - Messages describing any errors that occurred during sanitization.
   */
  public CleanResults(
      long startOfScan,
      Callable<String> cleanHTML,
      DocumentFragment XMLDocumentFragment,
      List<String> errorMessages) {
    this.startOfScan = startOfScan;
    this.elapsedScan = System.currentTimeMillis() - startOfScan;
    this.cleanHTML = cleanHTML;
    this.cleanXMLDocumentFragment = XMLDocumentFragment;
    this.errorMessages = Collections.unmodifiableList(errorMessages);
  }

  /**
   * Return the filtered HTML as a String. This output is the ONLY output you can trust to be safe.
   * The absence of error messages does NOT indicate the input was safe.
   *
   * @return A String object which contains the serialized, safe HTML.
   */
  public String getCleanHTML() {
    try {
      return cleanHTML.call();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Return the DOM version of the clean HTML.
   *
   * @return The XML Document fragment version of the clean HTML produced during the sanitization
   *     process. This may be null, even if the clean HTML String is not null.
   */
  public DocumentFragment getCleanXMLDocumentFragment() {
    return cleanXMLDocumentFragment;
  }

  /**
   * Return a list of error messages -- but an empty list returned does not mean there was no attack
   * present, due to the serialization and deserialization process automatically cleaning up some
   * attacks. Only the output of the {@code getCleanHTML()} should be considered safe. See the
   * project README file and {@code CleanResults} class documentation for further discussion.
   *
   * @return An ArrayList object which contains the error messages, if any, after a scan.
   * @see <a href="https://github.com/nahsra/antisamy/blob/main/README.md">Project README</a>
   * @see #getCleanHTML()
   */
  public List<String> getErrorMessages() {
    return errorMessages;
  }

  /**
   * Return the number of errors identified, if any, during filtering. Note that 0 errors does NOT
   * mean the input was safe. Only the output of {@code getCleanHTML()} can be considered safe.
   *
   * @return The number of errors encountered during filtering.
   * @see #getCleanHTML()
   */
  public int getNumberOfErrors() {
    return errorMessages.size();
  }

  /**
   * Return the time elapsed during the scan.
   *
   * @return A double indicating the amount of time elapsed between the beginning and end of the
   *     scan in seconds.
   */
  public double getScanTime() {
    return elapsedScan / 1000D;
  }

  /**
   * Get the time the scan started.
   *
   * @return time that scan started in milliseconds since epoch.
   */
  public long getStartOfScan() {
    return startOfScan;
  }
}
