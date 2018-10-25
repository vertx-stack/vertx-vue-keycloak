<template>
  <div class="users" v-on:keyup.enter="post">
    <div class="container">
      <h1>Message Wall</h1>
      <div id="socket-info" class="text-muted">Connected users: {{connectionCount}}</div>
      <div class="form-group row">
        <div class="col-sm-9">
          <input class="form-control" placeholder="Message" v-model="content" >
        </div>
        <div class="col-sm-3">
          <button class="btn btn-primary btn-block" @click="post">Send</button>
        </div>
      </div>
      <div class="row">
        <div class="col-sm-12">
          <ul class="list-group">
            <li v-for="message in messages" track-by="content" class="list-group-item">
             <span class="btn btn-sm label label-pill label-default pull-right" @click="remove(message)">
                <i class="fa fa-remove remove-cross"></i>
              </span>
              {{message.content}}
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script type="text/babel">
import axios from 'axios'
import eventbus from '../utils/eventbus'
import { remove } from '../utils/remove'
let self
export default {
  data () {
    self = this
    return {
      content: '',
      messages: [],
      connectionCount: 1
    }
  },
  methods: {
    getAll () {
      // call an API on the eventbus to receive all known messages from the backend
      eventbus.callApi('/api/messages', {}).then((response) => {
        this.messages = response
      })
    },
    post () {
      // call an API on the eventbus to post a new message to the backend
      if (self.content !== '') {
        eventbus.callApi('/api/messages/add', {content: self.content})
        self.content = ''
      }
    },
    remove (message) {
      // call an API on the eventbus to delete a message
      eventbus.callApi('/api/messages/delete', message)
    }
  },
  created() {
    // we allow a little delay until the vertxbus connects
    setTimeout(function () {
      // subscribe to client connection changes
      eventbus.subscribe(':pubsub/connections', function (message) {
        // console.log("count: ", message)
        if(message !== null) self.connectionCount = JSON.parse(message).count
      })

      // subscribe to message changes
      eventbus.subscribe(':pubsub/messages', function (message) {
        self.messages = message
      })
      
      // finally get all messages from the backend
      self.getAll()
    }, 500);
  }
}
</script>

<style lang="scss" scoped>

h1 {
  text-align: center;
  margin-top: 20px;
}

#socket-info {
  text-align: center;
  margin-bottom: 20px;
}
.row {
  padding-left: 10px;
  padding-right: 10px;
}
.form-inline {
  margin-top: 20px;
  margin-bottom: 20px;
}
.container {
  max-width: 500px;
}
.label-pill {
  border-radius: 10px;
  &:hover {
    background-color: #ff0000;
    .remove-cross {
      color: #fff;
    }
  }
}
.remove-cross {
  color: #aaa;
}
</style>
