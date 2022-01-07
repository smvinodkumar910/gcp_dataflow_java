/**
 * User-defined function (UDF) to transform events
 * as part of a Dataflow template job.
 *
 * @param {string} inJson input Pub/Sub JSON message (stringified)
 * @return {string} outJson output JSON message (stringified)
 */
 function process(inJson) {
    var obj = JSON.parse(inJson),
        includePubsubMessage = obj.data && obj.attributes,
        data = includePubsubMessage ? obj.data : obj;
    
    // INSERT CUSTOM TRANSFORMATION LOGIC HERE
  
    return JSON.stringify(obj);
  }