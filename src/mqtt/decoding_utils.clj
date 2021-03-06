(ns mqtt.decoding-utils
  (:import [java.nio.charset Charset]
           [java.io StreamCorruptedException]))

(defn assert-readable-bytes
  "Make sure that there are at least 'expected' more bytes to be read from the
  buffer. This is to protect against corrupt packets."
  [buffer expected]
  (let [remaining (.readableBytes buffer)]
    (when (< remaining expected)
      (throw (new StreamCorruptedException (str "Expected " expected " bytes, but only " remaining " remaining."))))))

(defn parse-unsigned-byte
  "Decode a single byte"
  [in]
  (assert-readable-bytes in 1)
  (.readUnsignedByte in))

(defn parse-unsigned-short
  "Decode an unsigned short"
  [in]
  (assert-readable-bytes in 2)
  (.readUnsignedShort in))

(defn- parse-flag
  [flags pos width]
  (let [max-val (int (- (Math/pow 2 width) 1))
        val (bit-and max-val (bit-shift-right flags pos))]
    (if (= 1 width)
      (= 1 val)
      val)))

(defn- do-parse-flags
  [flags m pos [key width & kvs]]
  (let [newpos (- pos width)
        ret (assoc m key (parse-flag flags newpos width))]
    (if kvs
      (recur flags ret newpos kvs)
      ret)))

(defn parse-flags
  "Decode a single byte of flags. Takes a list of pairs of keywords and
  bit-widths. All bit-widths must add up to 8. All 1-bit values are converted
  to boolean.

  Example:
  
    (parse-flags buffer :type 4, :dup 1, :qos 2, :retain 1)
  
  "
  [in & kvs]
  (do-parse-flags (parse-unsigned-byte in) {} 8 kvs))

(defn parse-string
  "Decode a utf-8 encoded string. Strings are preceeded by 2 bytes describing
  the length of the remaining content."
  [in]
  (let [len (int (parse-unsigned-short in))]
    (assert-readable-bytes in len)
    (.toString (.readBytes in len) (Charset/forName "UTF-8"))))
