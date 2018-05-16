/**
 *  Copyright 2018 Jonathan Augustine
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

package com.ampro.main.listener.events;

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;

/**
 * An wrapper class for {@code net.dv8tion.core.entities.Event}
 * that allows for easier
 * responding to messages in a number of ways; <br> namely,
 * methods for replying to messages regardless of the channel of origin or
 * responding to events directly to a {@code net.dv8tion.entites.PrivateChannel}
 *
 * @author Jonathan Augustine
 */
public abstract class BetterEvent {

    /**
     * Indicates the event used was not appropriate for
     * this implementation of BetterEvent.
     */
    public class InvalidEventException extends Exception {

        InvalidEventException() {
            super();
        }

        InvalidEventException(String s) {
            super(s);
        }

        public InvalidEventException(Throwable cause) {
            super(cause);
        }
    }

    public class InvalidAuthorException extends InvalidEventException {
        InvalidAuthorException(String s) { super(s); }
        public InvalidAuthorException() { super(); }
    }

    /** Get the original event */
    protected abstract Event getEvent();
    /** Get the author (User) of the event */
    protected abstract User getAuthor();
    /** Reply to the event in the channel of origin */
    protected abstract void reply(String message);
    /** Reply to the event in a private channel */
    protected abstract void privateReply(String message);
    /** Is the event from a Private chat? */
    protected abstract boolean isPrivate();

    /**
     * Construct a {@code BetterEvent} from a
     * {@code net.dv8tion.jda.core.entities.Event}
     * @param event Event to wrap
     */
    BetterEvent(Event event){}

    /** */
    public JDA getJDA() {
        return this.getEvent().getJDA();
    }

}
