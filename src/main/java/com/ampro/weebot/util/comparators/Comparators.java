package com.ampro.weebot.util.comparators;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.Member;

import java.util.Comparator;

public class Comparators {
    /** Compares a {@code Guild} buy it's IdLong */
    private static final class GuildComparator implements Comparator<Guild> {
        @Override
        public int compare(Guild g1, Guild g2) {
            return (int) (g1.getIdLong() - g2.getIdLong());
        }
    }

    /** Compares Users by their Unique IdLong */
    public static final class UserIdComparator implements Comparator<User> {
        @Override
        public int compare(User u1, User u2) {
            return (int) (u1.getIdLong() - u2.getIdLong());
        }
    }

    /** Compare Users by their name */
    private static final class UserNameComparator implements Comparator<User> {
        @Override
        public int compare(User u1, User u2) {
            return u1.getName().compareTo(u2.getName());
        }
    }

    /** Compares Members by their Unique IdLong */
    private static final class MemberIdComparator implements Comparator<Member> {
        @Override
        public int compare(Member m1, Member m2) {
            return (int) (m1.getUser().getIdLong() - m2.getUser().getIdLong());
        }
    }

    /** Compare Members by their {@code net.dv8tion.core.entities.Member.getEffectiveName()} */
    private static final class MemberNameComparator implements Comparator<Member> {
        @Override
        public int compare(Member m1, Member m2) {
            return m1.getEffectiveName().compareTo(m2.getEffectiveName());
        }
    }
}
