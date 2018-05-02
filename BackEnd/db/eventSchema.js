/*
eventSchema is the mongoose schema that defines the event type in the database
*/
let mongoose = require("mongoose")
let Schema = mongoose.Schema;
mongoose.Promise = global.Promise;
let eventSchema = Schema({
    name: {
        type: String,
        required: true},
    owner: {
        type: Schema.Types.ObjectId,
        ref: 'User',
        required: true
    },
    admin: [{
        type: Schema.Types.ObjectId,
        ref: 'User',
    }],
    identifier: {
        type: Number,
        unique: true
    },
    specificDates: {
        type: Boolean,
        required: true
    },
    dateRange: [Date],
    timeRange: [Number],
    expirationDate: {
        type: Date,
        required: false
    },
    accepted: [{
        type: Schema.Types.ObjectId,
        ref: 'User',
    }],
    invited: [{
        type: Schema.Types.ObjectId,
        ref: 'User',
    }],
    rejected: [{
        type: Schema.Types.ObjectId,
        ref: 'User',
    }],
    responses: {
        type: [{times: [{
            day: Date,
            timeRange: [Number]
        }],
        responder: {
            type: Schema.Types.ObjectId,
            ref: 'User',
        }
    }],
        default: []
    }
});

let meetEvent = mongoose.model('events', eventSchema);
module.exports = meetEvent;