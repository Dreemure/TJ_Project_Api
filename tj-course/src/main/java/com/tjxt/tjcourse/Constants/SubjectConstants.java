package com.tjxt.tjcourse.Constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

public class SubjectConstants {

    @AllArgsConstructor
    @Getter
    public enum Type {
        SIGNLE_CHOICE(1, "单选题"),
        MUtiple_CHOICE(2, "多选题"),
        NON_DIRECTIONAL_CHOICE(3, "不定向选择题"),
        JUDGEMENT_QUESTION(4, "判断题");
        private final Integer type;
        private final String desc;

        public static String desc(Integer subjectType) {
            for (Type type : values()) {
                if (Objects.equals(type.type, subjectType)) {
                    return type.desc;
                }
            }
            return null;
        }

    }

    @AllArgsConstructor
    @Getter
    public enum Difficult {
        EASY(1, "简单"), MEDIUM(2, "中等"), DIFFICULT(3, "困难");
        private final Integer type;
        private final String desc;

        public static String desc(Integer type) {
            for (Difficult difficult : values()) {
                if (Objects.equals(difficult.getType(), type)) {
                    return difficult.desc;
                }
            }
            return null;
        }
    }
}
