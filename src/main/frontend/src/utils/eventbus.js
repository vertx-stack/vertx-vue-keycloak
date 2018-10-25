import EventBus from 'vertx3-eventbus-client'

const API_URL = process.env.API_URL + '/eventbus' || 'http://localhost:8080/eventbus'
const eventbus = new EventBus(API_URL)

export default {
  callApi (api, inputObject) {
    return new Promise((resolve, reject) => {
      console.log('calling vertx API ', api)
      eventbus.send(api, inputObject, function (response, json) {
        resolve(json.body)
      })
    })
  },
  subscribe (api, bodyHandler) {
    eventbus.registerHandler(api, function (err, msg) {
      if (err) {
        console.log('SockJS/EventBus error: ', err)
      } else {
        bodyHandler(msg.body)
      }
    })
  }
}

eventbus.onopen = function () {
  this.isConnected = true
  console.log('EventBus is now connected')
}

eventbus.onerror = function (e) {
  console.log('General error: ', e)
}
