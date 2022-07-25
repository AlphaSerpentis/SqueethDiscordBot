// SPDX-License-Identifier: GPL-2.0-only

package space.alphaserpentis.squeethdiscordbot.data.server.squiz;

import java.util.Arrays;

public class SquizQuestions {
    public String question;
    public String answer;
    public String[] wrongAnswers;
    @Override
    public String toString() {
        return "question: " + question + "," +
                "answer: " + answer + "," +
                "wrongAnswers: " + Arrays.toString(wrongAnswers);
    }
}
