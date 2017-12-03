<template>
<div>
  <main role="main">
    <div class="jumbotron" vg-if="!showUploads">
      <div class="container">
        <!--UPLOAD-->
        <form enctype="multipart/form-data" novalidate v-if="isInitial || isSaving">
          <h4>Upload Torrent</h4>
          <div class="dropbox">
            <input type="file" multiple :name="uploadFieldName" :disabled="isSaving" @change="filesChange($event.target.name, $event.target.files); fileCount = $event.target.files.length" accept="application/x-bittorrent" class="input-file">
              <p v-if="isInitial">
                Drag your file(s) here to begin<br> or click to browse
              </p>
              <p v-if="isSaving">
                Uploading {{ fileCount }} files...
              </p>
          </div>
        </form>
        <hr>
        <form>
          <h4>Add Magnet</h4>
          <div class="form-row align-items-center">
            <div class="col-sm-11">
              <label class="sr-only" for="magnet">Magnet</label>
              <input type="text" :disabled="isSaving" v-model="magnet" class="form-control mb-2 mb-sm-0" id="magnet" placeholder="magnet://...">
            </div>
            <div class="col-auto">
              <button type="button" :disabled="!magnet || isSaving" @click="addMagnet(magnet)" class="btn btn-primary">Upload</button>
            </div>
          </div>
        </form>
      </div>
    </div>
    <div class="container">
      <div class="row">
        <table class="table">
          <thead class="thead-dark">
            <tr>
              <th scope="col">Name</th>
              <th scope="col">Peers</th>
              <th scope="col">Status</th>
              <th scope="col">Files</th>
              <th scope="col"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in uploadedFiles">
              <td>{{ item.name }}</td>
              <td>{{ item.stats.ActivePeers }}/{{ item.stats.TotalPeers }}</td>
              <td>{{ item.Downloaded | humanize }}/{{ item.Missing + item.Downloaded  | humanize }}</td>
              <td>
                  <ul class="list-unstyled">
                    <li v-for="file in item.files"><a :href=file.data >{{ file.Path.join('/') }} ({{file.Length | humanize}})</a></li>
                  </ul>
              </td>
              <td class="row">
                <div class="btn-group" role="group" aria-label="Actions">
                  <button @click="start(item.ih)" type="button" class="btn btn-sm btn-primary">Start</button>
                  <button @click="stop(item.ih)" type="button" class="btn btn-sm btn-warning">Pause</button>
                  <button @click="del(item.ih)" type="button" class="btn btn-sm btn-danger">Delete</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </main>
  <footer class="footer">
    <div class="container">
      <span class="text-muted">v{{ version.release }}@{{ version.buildTime }}</span>
    </div>
  </footer>
</div>
</template>

<script>
  import * as streamy from '@/streamy.service';
  import pb from 'pretty-bytes';
  const STATUS_INITIAL = 0, STATUS_SAVING = 1, STATUS_SUCCESS = 2, STATUS_FAILED = 3;

  export default {
    name: 'app',
    filters: {
      humanize: function (value) {
        return pb(value);
      }
    },
    data() {
      return {
        uploadedFiles: [],
        uploadError: null,
        fileCount: null,
        currentStatus: null,
        magnet: null,
        uploadFieldName: 'torrent',
        showUploads: false,
        version: {},
      }
    },
    computed: {
      isInitial() {
        return this.currentStatus === STATUS_INITIAL;
      },
      isSaving() {
        return this.currentStatus === STATUS_SAVING;
      },
      isSuccess() {
        return this.currentStatus === STATUS_SUCCESS;
      },
      isFailed() {
        return this.currentStatus === STATUS_FAILED;
      }
    },
    methods: {
      ping() {
        // reset form to initial state
        streamy.ping()
          .then(x => {
            this.version = x;
          });
      },
      list() {
        // reset form to initial state
        streamy.listTorrents()
          .then(x => {
            this.uploadedFiles = x;
          });
      },
      start(ih) {
        // reset form to initial state
        streamy.startTorrent(ih).then(x => {
            this.list();
        });
      },
      stop(ih) {
        // reset form to initial state
        streamy.stopTorrent(ih).then(x => {
            this.list();
        });
      },
      del(ih) {
        // reset form to initial state
        streamy.deleteTorrent(ih).then(x => {
            this.list();
        });
      },
      reset() {
        // reset form to initial state
        this.currentStatus = STATUS_INITIAL;
        this.uploadError = null;
        this.magnet = null;
        this.fileCount = null;
      },
      addMagnet(magnet) {
        this.currentStatus = STATUS_SAVING;
        streamy.addMagnet(magnet)
          .then(x => {
            this.list();
            this.reset();
          })
      },
      save(formData) {
        // upload data to the server
        this.currentStatus = STATUS_SAVING;

        streamy.uploadTorrent(formData)
          .then(x => {
            this.reset();
            this.list();
          })
          .catch(err => {
            this.reset();
          });
      },
      filesChange(fieldName, fileList) {
        // handle file changes
        if (!fileList.length) return;

        // append the files to FormData
        Array
          .from(Array(fileList.length).keys())
          .map(x => {
            var formData = new FormData();
            formData.append(fieldName, fileList[x], fileList[x].name);
            this.save(formData);
          });
      }
    },
    mounted() {
      this.reset();
      this.list();
      this.ping();
      setInterval(this.list, 5000);
    }
  }

</script>

<style lang="scss">
  $blue:    #2780E3 !default;

  body{
    padding-top: 4.5rem;
    margin-bottom: 30px; /* Margin bottom by footer height */
  }

  html {
    position: relative;
    min-height: 100%;
  }

  .footer {
    position: absolute;
    bottom: 0;
    width: 100%;
    height: 30px; /* Set the fixed height of the footer here */
    line-height: 30px; /* Vertically center the text there */
    background-color: #f5f5f5;
  }

  .dropbox {
    outline: 2px dashed grey; /* the dash box */
    outline-offset: -10px;
    background: lighten( $blue, 30% );
    color: dimgray;
    padding: 10px 10px;
    min-height: 200px; /* minimum height */
    position: relative;
    cursor: pointer;
  }

  .input-file {
    opacity: 0; /* invisible but it's there! */
    width: 90%;
    height: 200px;
    position: absolute;
    cursor: pointer;
  }

  .dropbox:hover {
    background: lighten( $blue, 25% ); /* when mouse over to the drop zone, change color */
  }

  .dropbox p {
    font-size: 1.2em;
    text-align: center;
    padding: 50px 0;
  }
</style>
