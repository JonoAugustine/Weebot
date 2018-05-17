package com.ampro.weebot.commands.games.cardgame;

/**
 * A card used in a {@code CardGame}.
 * Currenty holds no information in abstract,
 * Just here for future expansion and Generics
 */
abstract class Card {

    /** Just a little backup case in case something goes REALLY wrong. */
    protected static class InvalidCardException extends Exception {
        private static final long serialVersionUID = 7546072265632776147L;

        /** Parameterless constructor */
        public InvalidCardException() {
        }

        /** Constructor with message */
        public InvalidCardException(String err) {
            super(err);
        }
    }

}
