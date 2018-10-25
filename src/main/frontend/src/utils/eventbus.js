import EventBus from 'vertx3-eventbus-client'

const API_URL = process.env.API_URL + '/eventbus' || 'http://localhost:8080/eventbus'
console.log('EventBus API URL: ', API_URL)
const eventbus = new EventBus(API_URL)
// const eventbus = new EventBus('/eventbus')

/** Don't call until the event bus is open. */
function onopenEventBus () {
  // Call using event bus.
  eventbus.send('/api/messages', {name: 'tim', age: 587}, function (response, json) {
    console.log(json.body)
  })
}

/** Get notified of errors. */
function onerrorEventBus (error) {
  console.log('Problem calling event bus ', error)
}

eventbus.onopen = onopenEventBus
eventbus.onerror = onerrorEventBus

// const handlers = []

// eventbus.handle = function (url, bodyHandler) {
//   handlers.push({
//     url,
//     bodyHandler
//   })
// }

// eventbus.onopen = function () {
//   console.log(':: eventbus openOpen called')
//   handlers.forEach(handler => eventbus.registerHandler(handler.url, function (err, msg) {
//     console.log(':: handler called')
//     if (err) {
//       console.log('SockJS/EventBus error: ', err)
//     } else {
//       console.log('Received from EventBus: ', msg)
//       handler.bodyHandler(JSON.parse(msg.body))
//     }
//   }))
// }

export default eventbus
