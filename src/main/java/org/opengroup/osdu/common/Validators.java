//  Copyright © Microsoft Corporation
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.opengroup.osdu.common;

import org.apache.http.HttpStatus;
import org.opengroup.osdu.core.common.model.http.AppException;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A collection of useful validators.
 */
public final class Validators {

    private static final String WHITELISTED_CHARACTERS = "[-_[A-Za-z0-9]]";

    /**
     * This class is all static methods - no need for a constructor to be public.
     */
    private Validators() {
    }

    /**
     * Asserts that data is non null.
     *
     * @param data      The field which should not be null or empty
     * @param fieldName The name of the field, used for helpful exception
     *                  messaging
     * @throws NullPointerException if the data is null
     */
    public static void checkNotNull(
            final Object data, final String fieldName) {
        Objects.requireNonNull(data, fieldName + " cannot be null!");
    }

    /**
     * Throws an
     * Throws a {@link NullPointerException} if it is null.
     *
     * @param data      The field which should not be null or empty
     * @param fieldName The name of the field, used for helpful exception
     *                  messaging
     * @throws IllegalArgumentException  if the string is empty
     * @throws NullPointerException  if the string is null
     */
    public static void checkNotNullAndNotEmpty(
            final String data, final String fieldName) {
        checkNotNull(data, fieldName);
        if (data.chars().allMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException(fieldName + " cannot be empty!");
        }
    }

    /**
     * Throws an
     * Throws a {@link IllegalArgumentException} if it is not valid.
     *
     * @param data      The field which is to be verified
     * @throws AppException if the string does not have allowed chars
     */
    public static void checkValidDataPartition(
            final String data) {
        if (!checkValidPattern(data)) {
            throw new AppException(HttpStatus.SC_FORBIDDEN, "Invalid Data Partition Name", String.format("Data partition name: %s is not valid", data));
        }
    }

    /**
     * Helper function to check if the input string contains only the allowed characters
     * Alphanumeric characters and - and underscore...
     * We match the length of input string and the length of characters matched from the allowed list
     * @param input      The field which should not be null or empty
     * @return Boolean
     * */
    private static Boolean checkValidPattern(final String input) {
        Pattern pattern = Pattern.compile(WHITELISTED_CHARACTERS);
        Matcher matcher = pattern.matcher(input);
        int matchedCharacterCount = 0;
        while (matcher.find()) {
            matchedCharacterCount++;
        }
        return matchedCharacterCount == input.length();
    }

}
