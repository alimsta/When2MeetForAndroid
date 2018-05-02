/*
index serves as the server for the app. It handles all HTTP requests
that come from the app, responding with appropriate information
based on the state of the database.
*/ 
const express = require("express");
const bodyParser = require("body-parser");
const helmet = require("helmet");
const fs = require("fs");
const passport = require('passport');
const JWT = require('jsonwebtoken');
const passportJWT = require('passport-jwt');
const nodemailer = require('nodemailer');

const DBDriver = require("./db/DBDriver");

const port = process.env.PORT || 3000;
const secret = process.env.SECRET || "test";

let extractJWT = passportJWT.ExtractJwt;
let JWTStrategy = passportJWT.Strategy;

let jwtConfigOptions = {
    jwtFromRequest: extractJWT.fromBodyField("jwt"),
    secretOrKey: secret
};

passport.use(new JWTStrategy(jwtConfigOptions, function(payload, next) {
    DBDriver.validateId(payload.id).then((res) => {
        next(null, res);
    }).catch((err) => {
        next(err, false, {authentication: "FAILED"});
    });
}));

const app = express();

app.use(passport.initialize());
app.use(helmet());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({extended: true}));

/**
 * USER CREATION AND INITIAL VALIDATION 
 */


 // Check if a username has been used
 // Calls DBDriver.userNameExists(targetname)
 // @param {text} targetname 
 
app.post('/newUsername', async function (req, res) {
    try {
        if (req.body.text === '') {
            res.status(200);
            res.send('false');
        } else if (await DBDriver.userNameExists(req.body.text || req.query.text)) {
            res.status(200);
            res.send('true');
        } else {
            res.status(200);
            res.send('false');
        }
    } catch (error) {
        res.status(500);
        res.send(error);
    }
})

// Allow a user to create a new account
// Calls DBDriver.createUser(fullname, username)
// @param {fullname} fname 
// @param {uname} username

app.post('/newAccount', async function (req, res) {
    try {
        // will later need to add valid photopath as an argument
        let user = await DBDriver.createUser(req.body.fname, req.body.uname, req.body.pass,
            req.body.phone, req.body.email, '');
        let payload = {id: user._id};
        let token = JWT.sign(payload, jwtConfigOptions.secretOrKey);
        res.status(200);
        res.json({message: "ok", token: token});
    } catch (error) {
        res.status(500);
        res.send(error);
    }
})

// Allow a user to set a new password
// Calls DBDriver.resetPassword(userID, newPassword)
// @param {userID} userId 
// @param {newPassword} password

app.post('/newPassword', passport.authenticate('jwt', {session: false}), async function (req, res) {

    let token = req.body.jwt;
    let password = req.body.password;
    let userId;
    try {
        let decode = JWT.verify(token, secret);
        let id = decode.id;
        if(id === undefined) throw new Error("NO ID");
        userId = id;
    } catch (error) {
        res.status(400);
        res.json({error: "improperly formatted token"});
    }
    try {
        let returnVal = await DBDriver.resetPassword(userId, password);
        res.status(200);
        res.send(returnVal);
    }
    catch (error) {
        res.status(500);
        res.json({error: error.message});
    }
})

// Allow a user to sign in with valid username and password combo
// Calls DBDriver.authenticate(username, password)
// @param {username} uname 
// @param {password} pass

app.post('/signin', async function (req, res) {
    try {
        let uname = req.body.uname;
        let pass = req.body.pass;
        let id = await DBDriver.authenticate(uname, pass);
        if (id) {
            var payload = {id: id};
            var token = JWT.sign(payload, jwtConfigOptions.secretOrKey);
            res.status(200);
            res.json({message: "ok", token: token});
        } else {
            res.status(200);
            res.send('no user found');
        }
    } catch (error) {
        res.status(500);
        res.json({error: error.message});
    }
})

/**
 * EVENT CREATION AND MODIFICATION
 */

// Create a new event and save it to the database
// Calls DBDriver.getUserFromId(id)
// @param {id} id 

app.post("/createEvent", passport.authenticate('jwt', {session: false}), async (req, res) => {
    let eventName = req.body.eventName;
    let token = req.body.jwt;
    let specificDates = req.body.specificDates;
    let dateRange = req.body.dateRange;
    let timeRange = req.body.timeRange;
    let expirationDate = req.body.expirationDate;
    let invitedUsers = req.body.invitedUsers;
    if(eventName === undefined || token === undefined || specificDates === undefined
    || timeRange === undefined) {
        res.status(401);
        res.json({error: "insufficient arguments sent"});
    }
    let ownerUser;
    try {
        let decode = JWT.verify(token, secret);
        let id = decode.id;
        if(id === undefined) throw new Error("NO ID");
        ownerUser = (await DBDriver.getUserFromId(id)).userName;
        if(ownerUser === undefined || ownerUser === null) throw new Error("BAD ID");
    } catch (error) {
        res.status(402);
        res.json({error: "improperly formatted token"});
    }
    let dateArray = [];
    if(dateRange !== undefined && !Array.isArray(dateRange)) {
        console.log(dateRange);
        dateRange = splitArray(dateRange);
        console.log(dateRange);
    }
    
    if(timeRange !== undefined && !Array.isArray(timeRange)) {
        timeRange = splitArray(timeRange);
        for(let i = 0; i < timeRange.length; i++) {
            timeRange[i] = parseInt(timeRange[i]);
        }
    }
    console.log(invitedUsers);
    if(!(invitedUsers === undefined) && !Array.isArray(invitedUsers)) {
        invitedUsers = splitArray(invitedUsers);
    }
    if(specificDates) {
        try {
            if(dateRange === undefined || !Array.isArray(dateRange)) {        
                res.status(403);
                res.json({error: "improper date format4"});
            }
            else {
                dateRange.forEach(element => {
                    if(Date.parse(element) < Date.now - 86400000) {
                        res.status(404);
                        res.json({error: "improper date format2"});
                    }
                    dateArray.push(Date.parse(element));
                });
            }
        } catch (error) {
            res.status(405);
            res.json({error: "improper date format1"});
        }
    }
    if (expirationDate) {
        var timestamp = Date.parse(expirationDate);
        if (isNaN(timestamp)) {
            res.status(200);
            res.json({error: 'Expiration date not a real date'});
        }
    }
       
    try {
        let result = await DBDriver.createEvent(eventName, ownerUser, specificDates, dateArray, timeRange, 
            expirationDate, invitedUsers);
        res.status(201);
        res.json({eventName: result.name, eventOwner: result.owner, 
            specificDates: result.specificDates, eventId: result.identifier, error : null});
    } catch (error) {
        res.status(500);
        res.json({error: "error on event creation"});
    }
        
})

/**
 * CREATED EVENT INTERACTIONS
 */

// Gets users for an event
// Calls DBDriver.getUsers(eventId)
// @param {eventId} eventId 

app.post('/users', passport.authenticate('jwt', {session: false}), async (req, res) => {
    try {
        let eventId = req.body.eventId;
        if(eventId === undefined) {
            res.status(400);
            res.send("ERROR: EVENT ID REQUIRED");
        }
        let users = await DBDriver.getUsers(eventId);
        res.status(200);
        if(users === null) {
            res.json({error: "NOT FOUND"});
        }
        else {
            users.error = null;
            res.json(users);
        }
    } catch (error) {
        res.status(500);
        res.json({error: error.message});
    }
})

// Accept the user's responses for an event
// Calls DBDriver.addResonses(eventId, userId, responseArr)
// @param {eventId} eventId 
// @param {userId} userId
// @param {responseArr} resonseArr

app.post('/respond', passport.authenticate('jwt', {session: false}), async (req, res) => {
    try {
        let token = req.body.jwt;
        let dateRange = req.body.dateRange;
        let eventId = req.body.eventId;
        console.log(dateRange);
        if (eventId === undefined || token === undefined || dateRange === undefined) {
                res.status(401);
                res.json({error: "insufficient arguments sent"});
        }
        let userId = getIdFromToken(token);
        let responseArr = [];
        // parse date into a date object if 
        if(dateRange !== undefined && !Array.isArray(dateRange)) {
            dateRange = splitArray(dateRange);
        }
        try {
            if(dateRange === undefined || !Array.isArray(dateRange)) {        
                res.status(403);
                res.json({error: "improper date format4"});
            } else {
                let date;
                let times = [];
                for (let i = 0; i < dateRange.length; i++) {
                    let str = dateRange[i];
                    if (str.indexOf("D") > -1) {
                        if (i !== 0) responseArr.push({day: date, timeRange: times});
                        times = [];
                        str = str.slice(1, str.length);
                        console.log(parseInt(str));
                        date = parseInt(str);
                        console.log("DATE:  " + date);
                    } else {
                        times.push(parseInt(str));
                    }
                }
                responseArr.push({day: date, timeRange: times});
                let result = await DBDriver.addResponses(eventId, userId, responseArr);
                res.status(200);
                res.json({event: result.responses, error: "false"});
            }
        } catch (error) {
            res.status(405);
            res.json({error: error.message});
            console.log(error);
        }
    } catch (error) {
        res.status(500);
        res.json({error: error.message});
        console.log(error);
    }
})

// Retrieve the information associated with a given eventID
// Calls DBDriver.retrieveEvent(eventId)
// @param {eventId} eventId 

app.post('/event', passport.authenticate('jwt', {session: false}), async (req, res) => {
    try {
        let eventId = req.body.eventId;
        let token = req.body.jwt;
        if(eventId === undefined) {
            res.status(400);
            res.json({error: "ERROR: EVENT ID REQUIRED"});
        }
        let userId;
        try {
            let decode = JWT.verify(token, secret);
            let id = decode.id;
            if(id === undefined) throw new Error("NO ID");
            userId = id;
        } catch (error) {
            res.status(400);
            res.json({error: "improperly formatted token"});
        }
        let event = await DBDriver.retrieveEvent(eventId, userId);
        if(event === null) {
            res.status(200);
            res.json({error: "no such event"});
        }
        else {
            if(event.admin.indexOf(userId) !== -1) {
                delete event.admin;
                event.isOwner = event.owner._id == "" + userId;
                event.admin = true;
                event.error = "none";
                event.userInvited = false;
                res.status(200);
                res.json(event);
            }
            else {
                delete event.admin;
                event.isOwner = false;
                event.admin = false;
                event.error = "none";
                res.status(200);
                res.json(event);
            }
        }
    } catch (error) {
        res.status(500);
        res.json({error: error.message});
    }
})

// Invites a user to the specified event
// Calls DBDriver.inviteUser(eventId, userId, element)
// @param {eventId} eventId 
// @param {userId} userId
// @param {element} element

app.post('/invite', passport.authenticate('jwt', {session: false}), async(req, res) => {
    try {
        let eventId = req.body.eventId;
        let token = req.body.jwt;
        if(eventId === undefined) {
            res.status(400);
            res.json({error: "ERROR: EVENT ID REQUIRED"});
        }
        let userId;
        try {
            userId = getIdFromToken(token);
        } catch (error) {
            res.status(400);
            res.json({error: "improperly formatted token"});
        }
        let invited = req.body.invitedUsers;
        if(invited === undefined) {
            res.status(400);
            res.json({error: "no users invited"})
        }
        if(Array.isArray(invited)) {
            let promiseArray = [];
            invited.forEach((element) => {
                let invitePromise = DBDriver.inviteUser(eventId, userId, element);
                promiseArray.push(invitePromise);
            })
            for(let i = 0; i < promiseArray.length; i++) {
                promiseArray[i] = await promiseArray[i];
            }
            let success = false;
            const baseErrorString = "errors with names: ";
            let errorString = "errors with names: ";
            for(let i = 0; i < promiseArray.length; i++) {
                if(promiseArray[i] === null || !promiseArray[i]) {
                    errorString += invited[i];
                }
                else {
                    success = true;
                }
            }
            res.status(200);
            if(baseErrorString === errorString) {
                res.json({names: parsedInvited, error: "none"});
            }
            else {
                res.json({names: parsedInvited, error: errorString});
            }
        }
        else {
            if(invited.charAt(0) === '[') {
                let parsedInvited = splitArray(invited);
                console.log(parsedInvited + "");
                if(parsedInvited.length === 0) {
                    res.status(400);
                    res.json({error: "no users invited"})
                }
                else {
                    let promiseArray = [];
                    parsedInvited.forEach((element) => {
                        console.log("the element: " + element);
                        let invitePromise = DBDriver.inviteUser(eventId, userId, element);
                        promiseArray.push(invitePromise);
                    })
                    for(let i = 0; i < promiseArray.length; i++) {
                        promiseArray[i] = await promiseArray[i];
                    }
                    let success = false;
                    const baseErrorString = "errors with names: ";
                    let errorString = "errors with names: ";
                    for(let i = 0; i < promiseArray.length; i++) {
                        if(promiseArray[i] === null || !promiseArray[i]) {
                            errorString += parsedInvited[i];
                        }
                        else {
                            success = true;
                        }
                    }
                    res.status(200);
                    if(baseErrorString === errorString) {
                        res.json({names: parsedInvited, error: "none"});
                    }
                    else {
                        res.json({names: parsedInvited, error: errorString});
                    }
                }
            }
            else {
                let createPromise = await DBDriver.inviteUser(eventId, userId, invited);
                if(createPromise === null) {
                    res.status(200);
                    res.json({success: false, error: "creation request invalid"});
                }
                else {
                    res.status(200);
                    res.json({success: createPromise, error: "none"});
                }
            }
        }
    } catch (error) {
        res.status(500);
        res.json({error: error.message});
    }
})

// Invite someone without an account (non-user) to join the app by sending them an email
// Uses nodeMailer API

app.post('/inviteNonUser',  passport.authenticate('jwt', {session: false}), async(req, res) => {
    let token = req.body.jwt;

    try {
        let decode = JWT.verify(token, secret);
        let id = decode.id;
        if(id === undefined) throw new Error("NO ID");
    } catch (error) {
        res.status(400);
        res.json({error: "improperly formatted token"});
    }
    let email = req.body.email;

    // Generate test SMTP service account from ethereal.email
    // Only needed if you don't have a real mail account for testing
    var transporter = nodemailer.createTransport({
        service: 'gmail',
        auth: {
          user: 'when2meetmobile@gmail.com',
          pass: 'cis350g7'
        }
    });
      
    var mailOptions = {
        from: 'when2meetmobile@gmail.com',
        to: email,
        subject: 'You have been invited to when2meet mobile!',
        text: 'Your friend has invited you to an event on when2meet mobile, download now!'
    };
      
    transporter.sendMail(mailOptions, function(error, info){
        if (error) {
          console.log(error);
          res.status(200);
          res.send('false');
        } else {
          console.log('Email sent: ' + info.response);
          res.status(200);
          res.send('true');
        }
    });
})

// POST request test
app.post('/test', passport.authenticate('jwt', {session: false}), function (req, res) {
    res.send('success');
})

// Removes a guest from the event
// Calls DBDriver.removeGuest(eventId, userId)
// @param {eventId} eventId 
// @param {userId} userId

app.post('/removeGuest', passport.authenticate('jwt', {session: false}), async (req, res) => {
    try {
        let userID;
        let token = req.body.jwt;
        // Convert jwt to userID
        try {
            userID = getIdFromToken(token);
        } catch (error) {
            res.status(400);
            res.json({error: "improperly formatted token"});
        }
        let eventID = req.body.eventID;
        let removePromise = DBDriver.removeGuest(eventID, userID);
        let removeResult = await removePromise;
        if (removeResult === null) {
            res.json({error: "Guest not successfully removed"});
        }
        if (removeResult == "already left") {
            res.json({error: "Guest already left"});
        }
        else {
            res.send("Success"); // should send 'Success'
        }
    } catch (error) {
        res.status(500);
        res.json({error: error.message});
    }
})

// Allows a user to accept an invite to an event
// Calls DBDriver.addGuest(eventId, userId)
// @param {eventId} eventId 
// @param {userId} userId

app.post('/acceptInvite', passport.authenticate('jwt', {session: false}), async (req, res) => {
    try {
        let userID;
        let token = req.body.jwt; 
        // Convert jwt to userID
        try {
            userID = getIdFromToken(token);
        } catch (error) {
            res.status(400);
            res.json({error: "improperly formatted token"});
        }
        console.log("acceptInvite - userid: " + userID); 
        let eventID = req.body.eventID;
        let acceptInvitePromise = DBDriver.addGuest(eventID, userID);
        let acceptResult = await acceptInvitePromise;
        if (acceptResult == null) {
            res.json({error: "Guest did not successfully accept invitation"});
        }
        else {
            res.json({result: "Success"});
        }
    } catch (error) {
        res.status(500);
        res.json({error: error.message});
    }
})

// Allows a user to decline an invite to an event
// Calls DBDriver.declineInvite(eventId, userId)
// @param {eventId} eventId 
// @param {userId} userId

app.post('/declineInvite', passport.authenticate('jwt', {session: false}), async (req, res) => {
    try {
        let userId;
        let token = req.body.jwt; 
        // Convert jwt to userId
        try {
            userId = getIdFromToken(token);
        } catch (error) {
            res.status(400);
            res.json({error: "improperly formatted token"});
        }
        console.log("acceptInvite - userid: " + userId); 
        let eventId = req.body.eventId;
        let declineInvitePromise = DBDriver.declineInvite(eventId, userId);
        let declineResult = await declineInvitePromise;
        if (declineResult === null) {
            res.json({error: "Guest did not successfully decline invitation"});
        }
        else {
            res.json({result: "Success"});
        }
        
    } catch (error) {
        res.status(500);
        res.json({error: error.message});
    }
})

// Gets messages in user's inbox and sends back as string
// Calls DBDriver.getUserMessages(userId)
// @param {userId} userId

app.post('/getUserMessages', passport.authenticate('jwt', {session: false}), async (req, res) => {
    try {
        let userID;
        let token = req.body.jwt; 
        // Convert jwt to userID
        try {
            let decode = JWT.verify(token, secret);
            let id = decode.id;
            if(id === undefined) throw new Error("NO ID");
            userID = id;
        } catch (error) {
            res.status(400);
            res.json({error: "improperly formatted token"});
        }
        let getMessagesPromise = DBDriver.getUserMessages(userID);
        let messagesResult = await getMessagesPromise; 
        if (messagesResult === null) {
            res.json({error: "Did not successfully retrieve user's messages"});
        }
        else if (messagesResult === false) {
            res.json({result: "false"});
        }
        else {
            res.json(messagesResult);
        }
    } catch (error) {
        res.status(500);
        res.json({error: error.message});
    }
})

// Clears messages in user's inbox
// Calls DBDriver.clearUserMessages(userId)
// @param {userId} userId

app.post('/clearUserMessages', passport.authenticate('jwt', {session: false}), async (req, res) => {
    try {
        let userID;
        let token = req.body.jwt; 
        // Convert jwt to userID
        try {
            let decode = JWT.verify(token, secret);
            let id = decode.id;
            if(id === undefined) throw new Error("NO ID");
            userID = id;
        } catch (error) {
            res.status(400);
            res.json({error: "improperly formatted token"});
        }
        let clearMessagesPromise = DBDriver.clearUserMessages(userID);
        let messagesResult = await clearMessagesPromise;
        if (messagesResult === null) {
            res.json({error: "Did not successfully clear user's messages"});
        }
        else if (messagesResult === false) {
            res.json({result: "false"});
        }
        else {
            console.log("inside successful json object");
            res.json(messagesResult);
            //res.send("success");
        }
    } catch (error) {
        res.status(500);
        res.json({error: error.message});
    }
})

// Gets a user's admin events
// Calls DBDriver.getAdminEvents(userId)
// @param {userId} userId

app.post('/getAdminEvents', passport.authenticate('jwt', {session: false}), async (req, res) => {
    try {
        let token = req.body.jwt;
        let userId = getIdFromToken(token);
        let resObj = await DBDriver.getAdminEvents(userId);
        res.status(200);
        res.json({events: resObj});
    } catch (error) {
        res.status(500);
        res.json({error: error.message});
    }
})

// Allows an admin to delete an event
// Calls DBDriver.deleteEvent(eventId, userId)
// @param {eventId} eventID
// @param {userId} userID

app.post('/deleteEvent', passport.authenticate('jwt', {session: false}), async (req, res) => {
    try {
        let token = req.body.jwt;
        let userID = getIdFromToken(token);
        let eventID = req.body.eventID;
        await DBDriver.deleteEvent(eventID, userID);
        res.status(200);
        res.send('Success');
    } catch (error) {
        res.status(500);
        res.send({error: error.message});
    }
})

// Allows a user to get guest events
// Calls DBDriver.getGuestEvents(userId)
// @param {userId} userID

app.post('/getGuestEvents', passport.authenticate('jwt', {session: false}), async (req, res) => {
    try {
        let token = req.body.jwt;
        let userId = getIdFromToken(token);
        let resObj = await DBDriver.getGuestEvents(userId);
        res.status(200);
        res.json(resObj);
    } catch (error) {
        res.status(500);
        console.log(error.message);
        res.json({error: error.message});
    }
})

// Reminds users to respond to event invite
// Calls DBDriver.remindUsers(userId, eventId)
// @param {userId} userId
// @param {eventId} eventId

app.post('/remindUsers', passport.authenticate('jwt', {session: false}), async (req, res) => {
    try {
        let token = req.body.jwt;
        let userId = getIdFromToken(token);
        let eventId = req.body.eventId;
        let result = await DBDriver.remindUsers(userId, eventId);
        console.log("result of remindUsers is " + result);
        if (result == "Success") {
            res.send("Success");
        }
        else {
            res.send("Failure");
        }
    } catch (error) {
        res.status(500);
        res.json({error: error.message});
    }
})

app.post('/addAdmin', passport.authenticate('jwt', {session: false}), async (req, res) => {
    try {
        let token = req.body.jwt;
    let userId = getIdFromToken(token);
    let eventId = req.body.eventId;
    let targetName = req.body.targetName;
    let result = await DBDriver.addAdmin(userId, eventId, targetName);
    console.log(result);
    res.json(result);
    } catch (error) {
        res.json({success: false, error: error.message});
    }
})

app.listen(port, () => {
    console.log("SERVER RUNNING ON: " + port)
})

// Helper function that gets ID from token of corresponding user
// @param {token} user's token

function getIdFromToken(token) {
    let decode = JWT.verify(token, secret);
    let id = decode.id;
    if (id === undefined) throw new Error("NO ID");
    return id;
}

// Helper function that takes string input and splits into array
// @param {inputString} String of info returned from Front-End
function splitArray(inputString) {
    inputArray = inputString.split(", ");
    inputArray[0] = inputArray[0].substring(1);
    inputArray[inputArray.length - 1] = inputArray[inputArray.length - 1].substring(0, 
                                        inputArray[inputArray.length - 1].length - 1);
    return inputArray;
}
