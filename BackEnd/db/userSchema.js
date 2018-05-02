/*
userSchema is the mongoose schema that represents the user type inside the database
*/

let mongoose = require("mongoose")
let Schema = mongoose.Schema;
mongoose.Promise = global.Promise;

let userReferenceSchema = new Schema({
    user: {
        type: Schema.Types.ObjectId,
        ref: 'User'
    }
})

let userSchema = new Schema({
    name: String,
    userName: {
        type: String,
        unique: true
    },
    password: String,
    ownedEvents: [{
        type: Schema.Types.ObjectId,
        ref: 'events',
        default: []
    }],
    participantEvents: [{
        type: Schema.Types.ObjectId,
        ref: 'events',
        default:[]
    }],
    invitedEvents: [{
        type: Schema.Types.ObjectId,
        ref: 'events',
        default:[]
    }],
    messages: {
        type: [{
            message: String,
            eventIdentifier: Number
        }],
        default: []
    }
});

let user = mongoose.model('User', userSchema);
module.exports = user;