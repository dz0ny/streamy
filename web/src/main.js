import Vue from 'vue'
import App from './App'
import 'bootstrap/dist/css/bootstrap.css'
import Vue2Filters from 'vue2-filters'

Vue.use(Vue2Filters)
Vue.config.productionTip = false

/* eslint-disable no-new */
new Vue({
  el: '#app',
  render: h => h(App)
})
