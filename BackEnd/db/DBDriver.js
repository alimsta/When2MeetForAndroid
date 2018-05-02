/*
DBDriver is the class that contains all methods which access and
update information in the database. It contains methods related to 
both the eventSchema and userSchema.
*/
let user = require('./userSchema');
let event = require('./eventSchema');

let mongoose = require("mongoose");
let bcrypt = require("bcryptjs");

mongoose.Promise = global.Promise;

const MONGO_USER = process.env.MDB_USER || "user";
const MONGO_PASS = process.env.MDB_PASS || "pass";

const MONGO_URL = `mongodb://${MONGO_USER}:${MONGO_PASS}@ds041678.mlab.com:41678/cis_group_project`;

/** 
 * Program is DB dependent. Should exit if connection fails
*/
let connectionPromise = mongoose.connect(MONGO_URL).catch((err) => {
    console.log(err);
    process.exit(1);
})

let db = mongoose.connection;

class DBDriver{

    /**
     * USER ACCOUNT INTERACTIONS
     */

     /**
      * Checks if username is already taken
      * @param {String} targetName - desired username
      * @return {Promise<Boolean>}
      */
    static async userNameExists(targetName) {
        try {
            let me = await user.findOne({userName: targetName});
            return me !== null;
        } catch (error) {
            throw new Error("Error on username search");
        }
    }

    /**
     * Validates ID
     * @param {MongoID} jwtId 
     * @return {Promise<Boolean>}
     */
    static async validateId(jwtId) {
        try {
            return ((await user.findById(jwtId)) !== null)
        } catch (error) {
            throw new Error("Error on jwt auth");
        }
    }

    /**
     * get User Object from Id
     * @param {MongoID} Id
     * @return {UserModel} 
     */
    static async getUserFromId(Id) {
        try {
            return ((await user.findById(Id)));
        } catch (error) {
            throw new Error("Error on user retrieval");
        }
    }

    /**
     * Authenticates user
     * @param {String} username 
     * @param {String} password 
     * @return {MongoID}
     */
    static async authenticate(username, password) {
        try {
            let targetUser = await user.findOne({userName: username});
            if(targetUser === null) return null;
            let works = await bcrypt.compare(password, targetUser.password);
            if(!works) return false;
            else return targetUser._id;
        } catch (error) {
            console.log(error);
            throw new Error("Error on username search"); 
        }
    }
    
    /**
     * Creates a new user for sign-up
     * @param {String} fullName 
     * @param {String} userName 
     * @param {String} password 
     * @return {UserModels} 
     */
    static async createUser(fullName, userName, password) {
        try {
            let salt = await bcrypt.genSalt()
            let hashPromise = bcrypt.hash(password, salt);
            let newUser = new user();
            newUser.name = fullName;
            newUser.userName = userName;
            newUser.password = await hashPromise;
            return newUser.save()
        } catch (error) {
            console.log(error);
            throw new Error("Error on user creation");
        }
    }

    /**
     * Resets password for user with a new user-given one
     * @param {MongoID} userID 
     * @param {String} newPassword
     * @return {Boolean} 
     */
    static async resetPassword(userID, newPassword) {
        try {
            let saltPromise = bcrypt.genSalt();
            let result = await user.findByIdAndUpdate(userID, {password: await bcrypt.hash(newPassword, await saltPromise)});
            return result !== null;
        }
        catch (error) {
            console.log(error);
            throw new Error("Error on password change");
        }
    }

    /**
     * EVENT BASED INTERACTIONS
     */

     /**
      * CREATE EVENT
      * @param {String} eventName - Desired name of event
      * @param {String} eventCreatorUserName - user name of event creator
      * @param {Boolean} specificDates - defines whether the event if for specific dates
      * @param {[Date]} dateRange - the set of dates targeted
      * @param {[Number]} timeRange - the range of times for responses allowed
      * @param {Date} [expirationDate] - optional: expriation date of the event
      * @param {[String]} [invitedUserNames] - optional: usernames of users to be invited
      * @return {Promise<Event>} returns the object created by saving the input values
      */
    static async createEvent(eventName, eventCreatorUserName, specificDates, dateRange, timeRange, expirationDate, invitedUserNames) {
        try {
            let targetOwner = await user.findOne({userName: eventCreatorUserName});
            if(targetOwner === null) return null;
            let targetID = Math.round(Math.random() * 100000);
            while(await event.findOne({identifier: targetID}) !== null) {
                targetID = Math.round(Math.random() * 100000);
            }
            let newEvent = new event();
            newEvent.identifier = targetID;
            newEvent.name = eventName;
            newEvent.owner = targetOwner._id;
            newEvent.admin = [targetOwner._id];
            newEvent.specificDates = specificDates;
            newEvent.dateRange = dateRange;
            newEvent.timeRange = timeRange;
            if(expirationDate !== undefined) newEvent.expirationDate = expirationDate;
            if(invitedUserNames !== undefined && Array.isArray(invitedUserNames)) {
                for(let i = 0; i < invitedUserNames.length; i++) {
                    let targetUser = await user.findOne({userName: invitedUserNames[i]});
                    if(targetUser === null) continue;
                    newEvent.invited.push(targetUser._id);
                }
            }
            let saveResponse = await newEvent.save();
            targetOwner.ownedEvents.push(saveResponse._id);
            let savedOwnerPromise = targetOwner.save();
            if(invitedUserNames !== undefined) {
                for(let i = 0; i < invitedUserNames.length; i++) {
                    let targetUser = await user.findOne({userName: invitedUserNames[i]});
                    if(targetUser === null) continue;
                    targetUser.invitedEvents.push(saveResponse._id);
                    let newMessage = {};
                    let msg = "You have been invited to " + eventName + ". Click to accept/decline";
                    newMessage.message = msg;
                    newMessage.eventIdentifier = targetID;
                    targetUser.messages.push(newMessage) 
                    let targetUserPromise = await targetUser.save();
                    if (targetUserPromise == null) {
                        throw new Error("Could not save user message in event creation");
                    }
                }
            }
            return saveResponse;
        } catch (error) {
            console.log(error);
            throw new Error("Error on event creation");
        }
    }

    /**
     * Gets user for an event
     * @param {MongoID} eventID 
     * @return {{accepted: String[], invited: String[], rejected: String[]}}
     */
    static async getUsers(eventID) {
        try {
            let targetEventPopulated = await 
            event.findOne({identifier: eventID}).
            populate('accepted', 'name').
            populate('invited', 'name').
            populate('rejected', 'name').
            exec();
            if(targetEventPopulated === null) return null;
            let users = {};
            users.accepted = [];
            targetEventPopulated.accepted.forEach((element) => {
                if(element.name !== null) users.accepted.push(element.name);
            });
            users.invited = [];
            targetEventPopulated.invited.forEach((element) => {
                if(element.name !== null) users.invited.push(element.name);
            });
            if(users.invited.length > 0 && !!!users.invited[users.invited.length - 1]) {
                users.invited.splice(users.invited.length - 1, 1);
            }
            users.rejected = [];
            targetEventPopulated.rejected.forEach((element) => {
                if(element.name !== null) users.rejected.push(element.name);
            });
            return users;
        } catch (error) {
            throw new Error("Error on user population");
        }
    }

    /**
     * Save user responses to the specified event
     * @param {MongoID} eventID 
     * @param {userId} userID 
     * @return {event}
     */
    static async addResponses(eventID, userID, responsesArr) {
        try {
            let eventPromise = event.findOne({identifier: eventID});
            let targetEvent = await eventPromise;
            if (targetEvent === null) {
                return null;
            } 
            else {
                let responderIndex = targetEvent.responses.findIndex((element) => {
                    return element.responder == "" + userID;
                })
                if (responderIndex > -1) {
                    targetEvent.responses.splice(responderIndex, 1);
                }
                let currResponse = {times: responsesArr, responder: userID};
                targetEvent.responses.push(currResponse);
                let saveEvent = await targetEvent.save();

                console.log("addResponses: num responses: " + targetEvent.responses.length);
                console.log("addResponses: num accepted " + targetEvent.accepted.length);

                // Notify admin if everyone has responded to the event and all accepted guests have filled out the survey
                if (targetEvent.invited.length == 0 && (targetEvent.responses.length == targetEvent.accepted.length + 1)) {
                    console.log("addResponses: updating admin notif that all have responded.");
                    let adminPromise = user.findById(targetEvent.owner);
                    let eventAdmin = await adminPromise;
                    if (eventAdmin == null) {
                        console.log("addResponses: admin null");
                    }
                    let newMessage = {};
                    newMessage.message = "All guests have responded to your event: " + targetEvent.name;
                    newMessage.eventIdentifier = eventID;
                    eventAdmin.messages.push(newMessage);
                    let adminSavePromise = eventAdmin.save();
                    await adminSavePromise;
                }
                return saveEvent;
            }
        } catch (error) {
            console.log(error);
        }
    }

    /**
     * Gets event from Id
     * @param {MongoID} eventID 
     * @return {Object}
     * @property {String} evName
     * @property {String} adminName
     * @property {Boolean} specificDates
     * @property {Date[]} dateRange
     * @property {Number[]} timeRange
     * @property {{[{times: [{day: Date, timeRange: [Number]}], responder.name: String}}
     */
    static async retrieveEvent(eventID, userID) {
        try {
            let targetEventPopulated = await event.
            findOne({identifier: eventID}).
            populate('owner', 'name').
            populate({path: 'responses.responder', select: 'name'}).
            exec();
            if(targetEventPopulated === null) return null;
            let eventInformation = {};
            eventInformation.owner = targetEventPopulated.owner;
            eventInformation.evName = targetEventPopulated.name;
            eventInformation.adminName = targetEventPopulated.owner.name;
            eventInformation.admin = targetEventPopulated.admin;
            eventInformation.specificDates = targetEventPopulated.specificDates;
            eventInformation.dateRange = targetEventPopulated.dateRange;
            for(let i = 0; i < eventInformation.dateRange.length; i++) {
                eventInformation.dateRange[i] = Date.parse(eventInformation.dateRange[i]);
            }
            eventInformation.timeRange = targetEventPopulated.timeRange;
            eventInformation.responses = targetEventPopulated.responses;
            let newResponses = [];
            for(let j = 0; j < eventInformation.responses.length; j++){
                let newRes = {};
                let element = eventInformation.responses[j];
                newRes.responder = element.responder;
                newRes.times = []
                for(let i = 0; i < element.times.length; i++) {
                    newRes.times.push({day: Date.parse(element.times[i].day), 
                                        timeRange: element.times[i].timeRange});
                }
                newResponses.push(newRes);
            }
            eventInformation.responses = newResponses;
            eventInformation.invited = targetEventPopulated.invited;
            let targetIndex = targetEventPopulated.responses.findIndex((element) => {
                let match = (("" + userID) == element.responder._id)
               return element.responder._id == ("" + userID);
            })
            if (targetIndex === -1) {
                eventInformation.personalResponses = {times: [{
                    day: "",
                    timeRange: ""
                }], responder: ""};
            }
            else {
                eventInformation.personalResponses = targetEventPopulated.responses[targetIndex];
                let newResponses = {};
                newResponses.responder = eventInformation.personalResponses.responder;
                newResponses.times = [];
                for(let i = 0; i <  eventInformation.personalResponses.times.length; i++) {
                    let newRes = {};
                    newRes.day =  Date.parse(eventInformation.personalResponses.times[i].day);
                    newRes.timeRange = eventInformation.personalResponses.times[i].timeRange;
                    newResponses.times.push(newRes);
                }
                eventInformation.personalResponses = newResponses;
            }
            return eventInformation;
        } catch (error) {
            console.log(error);
            throw new Error("Error on user population");
        }
    }
    
    /**
     * Deletes specified event from database and notifies guests of the events that event
     * was deleted.
     * 
     * @param {MongoID} eventID 
     * @param {userId} userID 
     */
    static async deleteEvent(eventID, userID) {
        try {
            let eventPromise = event.findOneAndRemove({identifier: eventID, owner: userID});
            let targetEvent = await eventPromise;
            let ownerId = targetEvent.owner;
            let ownerObj = await user.findById(ownerId);
            let result = await user.findByIdAndUpdate(ownerId, {$pull: {ownedEvents: {_id: targetEvent._id}}});
            let acceptedList = targetEvent.accepted;
            for (let i = 0; i < acceptedList.length; i++) {
                let usersId = acceptedList[i];
                let userPromise = user.findById(usersId);
                let targetUser = await userPromise;
                let newMessage = {};
                let msg = "The event " + targetEvent.name + " has been deleted.";
                newMessage.message = msg;
                newMessage.eventIdentifier = eventID;
                targetUser.messages.push(newMessage);
                let userSavePromise = targetUser.save();
                await userSavePromise;
                await user.findByIdAndUpdate(usersId, {$pull: {participantEvents: {_id: targetEvent._id}}});
            }
            
            let invitedList = targetEvent.invited;
            for (let i = 0; i < invitedList.length; i++) {
                let usersId = invitedList[i];
                let userPromise = user.findById(usersId);
                let targetUser = await userPromise;
                let newMessage = {};
                let msg = "The event " + targetEvent.name + " has been deleted.";
                newMessage.message = msg;
                newMessage.eventIdentifier = eventID;
                targetUser.messages.push(newMessage);
                let userSavePromise = targetUser.save();
                await userSavePromise;
                await user.findByIdAndUpdate(usersId, {$pull: {invitedEvents: {_id: targetEvent._id}}});
            }
            
        
            event.remove({ _id : targetEvent._id});
        }
        catch (error) {
            console.log(error);
            throw new Error("Error on event deletion");
        }
    }
    
    /**
     * Invites a user to an event
     * @param {MongoID} eventID 
     * @param {userId} userID
     * @param {username} targetUsername
     * @return {boolean} valid invite
     */
    
    static async inviteUser(eventIdentifier, userID, targetUserName) {
        try {
            let eventPromise = event.findOne({identifier: eventIdentifier});
            let userPromise = user.findOne({userName: targetUserName});
            let targetEvent = await eventPromise;
            if(targetEvent === null || targetEvent.admin.indexOf(userID) === -1) {
                return null;
            }
            else {
                let targetUser = await userPromise;
                if(targetUser === null) {
                    return null;
                }
                else if(targetUser._id === userID) {
                    return null;
                }
                else {
                    let invitedIndex = targetEvent.invited.findIndex((element) => {
                        return element == targetUser._id + "";
                    })
                    if(invitedIndex !== -1) {
                        console.log("inviteUser: target user already invited");
                        return false;
                    }
                    let acceptedIndex = targetEvent.accepted.findIndex((element) => {
                        return element == targetUser._id + "";
                    })
                    if(acceptedIndex !== -1) {
                        console.log("inviteUser: target user already accepted");
                        return false;
                    }
                    let rejectedIndex = targetEvent.rejected.findIndex((element) => {
                        return element == targetUser._id + "";
                    })
                    if(rejectedIndex !== -1) {
                        console.log("inviteUser: target user rejected invitation...reinviting.");
                        targetEvent.rejected.splice(rejectedIndex, 1);
                    }
                    targetEvent.invited.push(targetUser._id);
                    targetUser.invitedEvents.push(targetEvent._id);

                    let newMessage = {};
                    let msg = "You have been invited to " + targetEvent.name + ". Click to accept/decline";
                    newMessage.message = msg;
                    newMessage.eventIdentifier = eventIdentifier;
                    targetUser.messages.push(newMessage);

                    let targetPromise = targetEvent.save();
                    
                    let userSavePromise = targetUser.save();
                    
                    let targetPromiseResult = await targetPromise;
                    let userSavePromiseResult = await userSavePromise;
                    return true;
                }
            }
        } catch (error) {
            
        }
    }
    
    
    /**
     * Removes a user from an event, where the user has already accepted the invite.
     * @param {MongoID} eventID 
     * @param {userId} userID
     * @return {boolean} function successfull
     */
    
    static async removeGuest(eventID, userID) {
            try {
            let eventPromise = event.findOne({identifier: eventID});
            let userPromise = user.findById(userID);
            let targetEvent = await eventPromise;
            // Error check for admin trying to leave his/her own event. 
            if (targetEvent === null) {
                return null;
            }
            if (userID == targetEvent.owner._id) {
                return null; 
            }
            else {
                let targetUser = await userPromise;
                if (targetUser === null) return null;
                else {
                    // Find the index of the the user within the accepted users of the event. 
                    let eventAcceptedIndex = targetEvent.accepted.findIndex((element) => {
                        return element == (targetUser._id + "");
                    })
                    if (eventAcceptedIndex == -1) return "already left";
                    // Remove user from the accepted users of the event. 
                    targetEvent.accepted.splice(eventAcceptedIndex, 1);

                    let adminIndex = targetEvent.admin.findIndex((element) => {
                        return element == (targetUser._id + "");
                    })
                    if (adminIndex !== -1) targetEvent.accepted.splice(adminIndex, 1);
                    // Find the index of the event within the accepted events of the user. 
                    let userAcceptedIndex = targetUser.participantEvents.findIndex((element) => {
                        return element == (targetEvent._id + "");
                    })
                    if (userAcceptedIndex == -1) return null;
                    // Remove event from the accepted events of the user. 
                    targetUser.participantEvents.splice(userAcceptedIndex, 1);

                    // Update notif for admin that someone has left his/her event.
                    let adminPromise = user.findById(targetEvent.owner);
                    let eventAdmin = await adminPromise;
                    if (eventAdmin == null) {
                        console.log("removeGuest: admin null");
                    }
                    let newMessage = {}
                    newMessage.message = targetUser.name + " has left your event " + targetEvent.name;
                    newMessage.eventIdentifier = eventID;
                    eventAdmin.messages.push(newMessage);
                    let adminSavePromise = eventAdmin.save();
                    let eventPromise = targetEvent.save();
                    let userPromise = targetUser.save();
                    await adminSavePromise;
                    await eventPromise;
                    await userPromise;
                    return true;
                }            
            }  
        } catch (error) {
            console.log(error);
            throw new Error("Error on guest removal inside DBDriver");
        }
    }
    
    /**
     * Adds guest to an event
     * @param {MongoID} eventID 
     * @param {userId} userID
     * @return {boolean} function successful
     */

    static async addGuest(eventID, userID) {
        try {
            let eventPromise = event.findOne({identifier: eventID});
            let userPromise = user.findById(userID);
            let targetEvent = await eventPromise;
            if (userID == targetEvent.owner._id) return null; 
            if (targetEvent === null) return null;
            else {
                let targetUser = await userPromise;
                if (targetUser === null) return null;
                else {
                    // Find the index of the user within the rejected users of the event.
                    let eventRejectedIndex = targetEvent.rejected.findIndex((element) => {
                        return element === targetUser._id;
                    })
                    // Return if user is already in the rejected guest list
                    if (eventRejectedIndex !== -1) {
                        console.log("addGuest: guest already rejected the event");
                        return null;
                    }
                    // Find the index of the the user within the accepted users of the event. 
                    let eventAcceptedIndex = targetEvent.accepted.findIndex((element) => {
                        return element === targetUser._id;
                    })
                    // Return if user is already in the accepted guest list
                    if (eventAcceptedIndex !== -1) {
                        console.log("addGuest: guest has already accepted invitation");
                        return null;
                    }

                    let eventInvitedIndex = targetEvent.invited.findIndex((element) => {
                        return element == (targetUser._id + "");
                    })
                    console.log("addGuest: event invited index = " + eventInvitedIndex);
                    // Return if user is not an invitee to the event
                    if (eventInvitedIndex === -1) {
                        return null;
                    }
                    // Remove user from the invitee list 
                    targetEvent.invited.splice(eventInvitedIndex, 1);
                    // Add invited user to the accepted users of the event. 
                    targetEvent.accepted.push(targetUser._id);
                    
                    // If invited list is empty and everyone has responded, then notify the owner that everyone has responded
                    if (targetEvent.invited.length == 0 && (targetEvent.responses.length == targetEvent.accepted.length + 1)) {
                        let adminPromise = user.findById(targetEvent.owner);
                        let eventAdmin = await adminPromise;
                        if (eventAdmin == null) {
                            console.log("addGuest: admin null");
                        }
                        let newMessage = {};
                        newMessage.message = "All guests have responded to your event: " + targetEvent.name;
                        newMessage.eventIdentifier = eventID;
                        eventAdmin.messages.push(newMessage);
                        let adminSavePromise = eventAdmin.save();
                        await adminSavePromise;
                    }
                    
                    // Update the user's fields: invitedEvents, participantEvents 
                    let userInvitedIndex = targetUser.invitedEvents.findIndex((element) => {
                        return element == (targetEvent._id + "");
                    })
                    if (userInvitedIndex == -1) {
                        console.log("addGuest: targetUser was not invited to the event.");
                        return null;
                    }
                    
                    targetUser.invitedEvents.splice(userInvitedIndex, 1);
                    let userAcceptedIndex = targetUser.participantEvents.findIndex((element) => {
                        return element === targetEvent._id;
                    })
                    if (userAcceptedIndex === -1) {
                        targetUser.participantEvents.push(targetEvent._id);
                    }
                    let adminPromise = user.findById(targetEvent.owner);
                    let eventAdmin = await adminPromise;
                    if (eventAdmin == null) {
                        console.log("addGuest: admin null");
                    }
                    let newMessage = {};
                    newMessage.message = targetUser.name + " has joined your event " + targetEvent.name;
                    newMessage.eventIdentifier = eventID;
                    eventAdmin.messages.push(newMessage);
                    let adminSavePromise = eventAdmin.save();

                    let eventPromise = targetEvent.save();
                    let userPromise = targetUser.save();
                    await adminSavePromise;
                    await eventPromise;
                    await userPromise;
                    return true;
                }            
            }
        } catch (error) {
            console.log(error);
            throw new Error("Error on event invite acceptance inside DBDriver");
        }
    }
    
    /**
     * Gets user message from inboxx
     * @param {userId} userID
     * @return {userMessages} specified user's messages
     */

    static async getUserMessages(userID) {
        try {
            let userPromise = user.findById(userID);
            let targetUser = await userPromise;
            if (targetUser === null) return null;
                else {
                    if (targetUser.messages == []) {
                        return false;
                    }
                    if (targetUser.messages == undefined) {
                        return null;
                    }
                    let userMessages = {};
                    userMessages.messages = targetUser.messages;
                    
                    // Clear the messages of the user
                    targetUser.messages = [];
                    let userSavePromise = targetUser.save();
                    let saveResult = await userSavePromise;
                    if (saveResult !== null) {
                        return userMessages;
                    }
                    else {
                        return false;
                    }
                }
        } catch (error) {
            console.log(error);
            throw new Error("Error on getUserMessages inside DBDriver");
        }
    }
    
    /**
     * Clears a user's messages
     * @param {userId} userID
     * @return {boolean} whether user messages are empty
     */
    
    static async clearUserMessages(userID) {
        try {
            let userPromise = user.findById(userID);
            let targetUser = await userPromise;
            if (targetUser === null) return null;
                else {
                    if (targetUser.messages == []) {
                        return true;
                    }
                    if (targetUser.messages == undefined) {
                        return null;
                    }
                    targetUser.messages = [];
                    let userSavePromise = targetUser.save();
                    let saveResult = await userSavePromise;
                    if (saveResult !== null) return true;
                    else return false;
                }
        } catch (error) {
            console.log(error);
            throw new Error("Error on getUserMessages inside DBDriver");
        }
    }
    
    /**
     * Gets a user's admin events
     * @param {targetId} userID
     * @return {retArray} array of admin events IDs
     */

    static async getAdminEvents(targetId) {
        try {
            let targetEvents = await event.find({admin: targetId});
            let retArray = [];
            if(targetEvents !== null) {
                targetEvents.forEach((element) => {
                    let targetObj = {};
                    targetObj.eventName = element.name;
                    targetObj.identifier = element.identifier;
                    retArray.push(targetObj);
                })
            }
            return retArray;
        } catch (error) {
            console.log(error);
            throw new Error("Error on get admin events");
        }
    }
    
    /**
     * Gets a user's guest events
     * @param {targetId} userID
     * @return {retArray} array of guest events IDs
     */

    static async getGuestEvents(targetID) {
        try {
            let userPromise = user.findById(targetID).
                                populate('invitedEvents').
                                populate('participantEvents').
                                exec();
            let targetUser = await userPromise;
            if(targetUser === null || targetUser === undefined) throw new Error('invalid user');
            let retObj = {};
            let invitedArr = [];
            let acceptedArr = [];
            targetUser.invitedEvents.forEach((element) => {
                let targetElement = {};
                targetElement.identifier = element.identifier;
                targetElement.name = element.name;
                invitedArr.push(targetElement);
            })

            targetUser.participantEvents.forEach((element) => {
                let targetElement = {};
                targetElement.identifier = element.identifier;
                targetElement.name = element.name;
                acceptedArr.push(targetElement);
            })

            retObj.invited = invitedArr;
            retObj.accepted = acceptedArr;
            return retObj;
        } catch (error) {
            console.log(error);
            throw new Error("Error on getUserMessages inside DBDriver");
        }
    }
    
    /**
     * Allows a user to decline an invite to an event
     * @param {eventId} eventID
     * @param {userId} userID
     * @return {boolean} whether function was fully run through
     */

    static async declineInvite(eventId, userId) {
        try {
            let eventPromise = event.findOne({identifier: eventId});
            let userPromise = user.findById(userId);
            let targetEvent = await eventPromise;
            // Error check for admin decline invite from his/her own event. 
            if (targetEvent.admin.indexOf(userId) !== -1) {
                return null; 
            }
            if (targetEvent === null) {
                return null;
            }
            else {
                let targetUser = await userPromise;
                if (targetUser === null) return null;
                else {
                    // Find the index of the the user within the invited users of the event. 
                    let eventInvitedIndex = targetEvent.invited.findIndex((element) => {
                        return element == (targetUser._id + "");
                    })
                    if (eventInvitedIndex === -1) {
                        console.log("declineInvite: guest not invited");
                        return null;
                    }
                    // Remove user from the invited users of the event. 
                    targetEvent.invited.splice(eventInvitedIndex, 1);
                    // Find the index of the event within the invited events of the user. 
                    let userInvitedIndex = targetUser.invitedEvents.findIndex((element) => {
                        return element == (targetEvent._id + "");
                    })
                    if (userInvitedIndex === -1) {
                        console.log("declineInvite: event was not in the invited events of the user");
                        return null;
                    }
                    // Remove event from the invited events of the user. 
                    targetUser.invitedEvents.splice(userInvitedIndex, 1);

                    // Add user to rejected users of event
                    targetEvent.rejected.push(targetUser);

                    // Update notif for admin that someone has declined his/her event. 
                    let adminPromise = user.findById(targetEvent.owner);
                    let eventAdmin = await adminPromise;
                    if (eventAdmin == null) {
                        console.log("removeGuest: admin null");
                    }
                    let newMessage = {}
                    newMessage.message = targetUser.name + " has declined your event invite for " + targetEvent.name;
                    newMessage.eventIdentifier = eventId;
                    eventAdmin.messages.push(newMessage);

                    // If invited list is empty, then notify the owner that everyone has responded
                    if ((targetEvent.invited.length == 0) && (targetEvent.responses.length == targetEvent.accepted.length + 1)) {
                        let newMessageEventDone = {};
                        newMessageEventDone.message = "All guests have responded to your event: " + targetEvent.name;
                        newMessageEventDone.eventIdentifier = eventId;
                        eventAdmin.messages.push(newMessageEventDone);
                    }

                    let adminSavePromise = eventAdmin.save();
                    let eventPromise = targetEvent.save();
                    let userPromise = targetUser.save();
                    await adminSavePromise;
                    await eventPromise;
                    await userPromise;
                    console.log("declineInvite: returning true");
                    return true;
                }            
            }  
        } catch (error) {
            console.log(error);
            throw new Error("Error on guest removal inside DBDriver");
        }
    }
    
    /**
     * Reminds an invited guest of an event to respond
     * @param {userId} userID
     * @param {eventId} eventID
     * @return {String} indicates result of method call
     */

    static async remindUsers(userId, eventId) {
        try {
        // Retrieve event and user
        let eventPromise = event.findOne({identifier: eventId, owner: userId}).
                            populate('owner', 'name').exec();
        let targetEvent = await eventPromise;

        // Error check for to make sure admin is making the request (only one allowed to) 
        // TODO: CORRECT FOR LIST
        if (targetEvent === null) {
            console.log("Event cannot be found");
            return "Event cannot be found";
        }
        

        let eventName = targetEvent.name;
        let newMessage = {};
        newMessage.message = targetEvent.owner.name + " has sent you a reminder to update/finalize your availability poll for the event: " + eventName;
        newMessage.eventIdentifier = eventId;

        // Remind all users who are invited to the event
        for (let i = 0; i < targetEvent.invited.length; i++) {
            let innerUserPromise = user.findById(targetEvent.invited[i]);
            let innerUser = await innerUserPromise;
            if (innerUserPromise === null) {
                console.log("remindUsers: innerUserPromise invited null");
                return "User could not be found";
            }
            innerUser.messages.push(newMessage);
            let updateMsgPromise = innerUser.save();
            let updateResult = await updateMsgPromise;
            if (updateResult === null) {
                let errorMsg = "Could not update " + targetEvent.invited[i].name +"'s messages.";
                console.log("remindUsers: " + errorMsg);
                return errorMsg; 
            }
        }

        // Remind all users who have accepted the event
        for (let i = 0; i < targetEvent.accepted.length; i++) {
            let innerUserPromise = user.findById(targetEvent.invited[i]);
            let innerUser = await innerUserPromise;
            if (innerUserPromise === null) {
                console.log("remindUsers: innerUserPromise accepted null");
                return "User could not be found";
            }
            innerUser.messages.push(newMessage);
            let updateMsgPromise = innerUser.save();
            let updateResult = await updateMsgPromise;
            if (updateResult === null) {
                let errorMsg = "Could not update " + targetEvent.accepted[i].name +"'s messages.";
                console.log("remindUsers: " + errorMsg);
                return errorMsg; 
            }
        }
        return "Success";
    } catch (error) {
            console.log(error);
            throw new Error("Error on remindUsers inside DBDriver");
        }
    }
    
    static async addAdmin(userId, eventId, targetUsername) {
        try {
            let targetEventPromise = event.findOne({identifier: eventId, owner: userId});
            let targetNewAdminPromise = user.findOne({userName: targetUsername});
            let targetEvent = await targetEventPromise;
            let targetAdmin = await targetNewAdminPromise;
            if(targetAdmin === null || targetEvent === null) {
                return({success: false, error: "target not found"});
            }
            if(targetEvent.accepted.indexOf(targetAdmin._id) === -1) {
                return({success: false, error: "target has not accepted"});
            }

            if(targetEvent.admin.indexOf(targetAdmin._id) === -1) {
                targetEvent.admin.push(targetAdmin._id);
                await targetEvent.save();
                return({success: true, error: ""});
            }
            return ({success: false, error: "Already admin"});
        } catch (error) {
            throw new Error("Error on add new admin inside DBDriver");
        }
    }

}

module.exports = DBDriver;