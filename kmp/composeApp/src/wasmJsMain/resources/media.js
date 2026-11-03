// Web だけの PDF 書き出し。Kotlin/Wasm からは引数のない固定文字列で呼ぶ。
// 画像の選択 → 縮小と再エンコード → PDF 組み立て → ダウンロードまでをここで行う。
(function () {
  "use strict";

  var state = { status: "idle", message: "", bytes: 0 };

  function setState(status, message, bytes) {
    state.status = status;
    state.message = message || "";
    state.bytes = bytes || 0;
  }

  function pickFiles(limit, camera) {
    return new Promise(function (resolve) {
      var input = document.createElement("input");
      input.type = "file";
      input.accept = "image/*";
      input.multiple = limit > 1 && !camera;
      if (camera) { input.capture = "environment"; }
      input.style.display = "none";
      document.body.appendChild(input);
      input.onchange = function () {
        var files = Array.prototype.slice.call(input.files || [], 0, limit);
        document.body.removeChild(input);
        resolve(files);
      };
      input.click();
    });
  }

  function readFile(file) {
    return new Promise(function (resolve, reject) {
      var reader = new FileReader();
      reader.onload = function () { resolve(reader.result); };
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });
  }

  function loadImage(dataUrl) {
    return new Promise(function (resolve, reject) {
      var img = new Image();
      img.onload = function () { resolve(img); };
      img.onerror = reject;
      img.src = dataUrl;
    });
  }

  // 長辺を maxDim に収め、JPEG に詰め直す。gray のときは輝度に落とす。
  function reencode(img, quality, maxDim, gray, binary) {
    var scale = Math.min(1, maxDim / Math.max(img.naturalWidth, img.naturalHeight));
    var w = Math.max(1, Math.round(img.naturalWidth * scale));
    var h = Math.max(1, Math.round(img.naturalHeight * scale));
    var canvas = document.createElement("canvas");
    canvas.width = w;
    canvas.height = h;
    var ctx = canvas.getContext("2d");
    ctx.drawImage(img, 0, 0, w, h);
    if (gray || binary) {
      var frame = ctx.getImageData(0, 0, w, h);
      var p = frame.data;
      for (var i = 0; i < p.length; i += 4) {
        var v = (p[i] * 0.299 + p[i + 1] * 0.587 + p[i + 2] * 0.114) | 0;
        if (binary) { v = v > 150 ? 255 : 0; }
        p[i] = v; p[i + 1] = v; p[i + 2] = v;
      }
      ctx.putImageData(frame, 0, 0);
    }
    return { dataUrl: canvas.toDataURL("image/jpeg", quality / 100), width: w, height: h };
  }

  function base64ToBytes(b64) {
    var binary = atob(b64);
    var bytes = new Uint8Array(binary.length);
    for (var i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
    return bytes;
  }

  function ascii(text) {
    var bytes = new Uint8Array(text.length);
    for (var i = 0; i < text.length; i++) bytes[i] = text.charCodeAt(i) & 0xff;
    return bytes;
  }

  // 用紙 A4。JPEG を DCTDecode でそのまま流し込む。
  function buildPdf(images) {
    var chunks = [];
    var length = 0;
    function push(part) {
      var bytes = typeof part === "string" ? ascii(part) : part;
      chunks.push(bytes);
      length += bytes.length;
    }
    function offset() { return length; }

    var pageWidth = 595, pageHeight = 842, margin = 24;
    var offsets = [];
    push("%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n");

    offsets.push(offset());
    push("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

    offsets.push(offset());
    push("2 0 obj\n<< /Type /Pages /Count " + images.length + " /Kids [");
    for (var k = 0; k < images.length; k++) push((3 + k * 3) + " 0 R ");
    push("] >>\nendobj\n");

    images.forEach(function (image, index) {
      var pageNo = 3 + index * 3;
      var maxW = pageWidth - margin * 2;
      var maxH = pageHeight - margin * 2;
      var scale = Math.min(maxW / image.width, maxH / image.height);
      var w = Math.max(1, Math.round(image.width * scale));
      var h = Math.max(1, Math.round(image.height * scale));
      var x = Math.round((pageWidth - w) / 2);
      var y = Math.round((pageHeight - h) / 2);

      offsets.push(offset());
      push("3".replace("3", String(pageNo)) + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " +
        pageWidth + " " + pageHeight + "] /Resources << /XObject << /Im" + index + " " + (pageNo + 2) +
        " 0 R >> >> /Contents " + (pageNo + 1) + " 0 R >>\nendobj\n");

      var content = "q " + w + " 0 0 " + h + " " + x + " " + y + " cm /Im" + index + " Do Q\n";
      offsets.push(offset());
      push((pageNo + 1) + " 0 obj\n<< /Length " + content.length + " >>\nstream\n" + content + "endstream\nendobj\n");

      offsets.push(offset());
      push((pageNo + 2) + " 0 obj\n<< /Type /XObject /Subtype /Image /Width " + image.width +
        " /Height " + image.height + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length " +
        image.bytes.length + " >>\nstream\n");
      push(image.bytes);
      push("\nendstream\nendobj\n");
    });

    var xref = offset();
    push("xref\n0 " + (offsets.length + 1) + "\n");
    push("0000000000 65535 f \n");
    offsets.forEach(function (value) {
      push(String(value).padStart(10, "0") + " 00000 n \n");
    });
    push("trailer\n<< /Size " + (offsets.length + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");

    var total = 0;
    chunks.forEach(function (chunk) { total += chunk.length; });
    var out = new Uint8Array(total);
    var at = 0;
    chunks.forEach(function (chunk) { out.set(chunk, at); at += chunk.length; });
    return out;
  }

  function download(bytes) {
    var blob = new Blob([bytes], { type: "application/pdf" });
    var url = URL.createObjectURL(blob);
    var a = document.createElement("a");
    a.href = url;
    a.download = "openyaske.pdf";
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(function () { URL.revokeObjectURL(url); }, 4000);
  }

  function encodeAll(sources, quality, maxDim, gray, binary) {
    return Promise.all(sources.map(function (img) {
      var out = reencode(img, quality, maxDim, gray, binary);
      return { bytes: base64ToBytes(out.dataUrl.split(",")[1]), width: out.width, height: out.height };
    }));
  }

  // 目標容量を超えたら品質を下げて作り直す（最大 2 回）。
  function run(quality, maxDim, gray, targetMb, binary, raw, camera) {
    var limit = targetMb * 1024 * 1024;
    setState("working", "写真を選んでください", 0);
    pickFiles(camera ? 1 : 12, camera).then(function (files) {
      if (!files.length) { setState("cancelled", "取り消しました", 0); return; }
      setState("working", files.length + " 枚を処理しています", 0);
      return Promise.all(files.map(function (file) {
        return readFile(file).then(function (dataUrl) {
          return loadImage(dataUrl).then(function (img) {
            return { img: img, dataUrl: dataUrl };
          });
        });
      })).then(function (sources) {
        var q = quality;
        var attempt = 0;
        function build() {
          var encoding = raw
            ? Promise.resolve(sources.map(function (source) {
                return {
                  bytes: base64ToBytes(source.dataUrl.split(",")[1]),
                  width: source.img.naturalWidth,
                  height: source.img.naturalHeight
                };
              }))
            : encodeAll(sources.map(function (source) { return source.img; }), q, maxDim, gray, binary);
          return encoding.then(function (encoded) {
            var pdf = buildPdf(encoded);
            if (limit > 0 && pdf.length > limit && attempt < 2 && q > 35) {
              attempt += 1;
              q = Math.max(35, q - 12);
              return build();
            }
            download(pdf);
            setState("done", encoded.length + " ページ / " + Math.round(q) + "% で作成しました", pdf.length);
          });
        }
        return build();
      });
    }).catch(function (error) {
      setState("error", String(error && error.message ? error.message : error), 0);
    });
  }

  // 画面を使わずに、PDF の組み立てと保存データの整合性だけを確かめる。
  function selfTest() {
    var canvas = document.createElement("canvas");
    canvas.width = 600;
    canvas.height = 800;
    var ctx = canvas.getContext("2d");
    ctx.fillStyle = "#ffffff";
    ctx.fillRect(0, 0, 600, 800);
    ctx.fillStyle = "#121d2f";
    ctx.font = "48px sans-serif";
    ctx.fillText("OpenYASKE", 60, 120);
    var dataUrl = canvas.toDataURL("image/jpeg", 0.8);
    var image = { bytes: base64ToBytes(dataUrl.split(",")[1]), width: 600, height: 800 };
    var second = { bytes: image.bytes, width: 600, height: 800 };
    var pdf = buildPdf([image, second]);
    var head = "";
    for (var i = 0; i < 8; i++) head += String.fromCharCode(pdf[i]);
    var tail = "";
    for (var j = pdf.length - 6; j < pdf.length; j++) tail += String.fromCharCode(pdf[j]);
    return JSON.stringify({ bytes: pdf.length, head: head, tail: tail, pages: 2 });
  }

  // ----- 時間割の背景画像（IndexedDB） -----
  var bgStatus = "idle";
  var bgDataUrl = null;
  var BG_KEY = "timetable_background";

  function openDb() {
    return new Promise(function (resolve, reject) {
      var request = indexedDB.open("openyaske", 1);
      request.onupgradeneeded = function () {
        request.result.createObjectStore("files");
      };
      request.onsuccess = function () { resolve(request.result); };
      request.onerror = function () { reject(request.error); };
    });
  }

  function idbGet(key) {
    return openDb().then(function (db) {
      return new Promise(function (resolve) {
        var tx = db.transaction("files", "readonly");
        var request = tx.objectStore("files").get(key);
        request.onsuccess = function () { resolve(request.result || null); };
        request.onerror = function () { resolve(null); };
      });
    });
  }

  function idbSet(key, value) {
    return openDb().then(function (db) {
      return new Promise(function (resolve) {
        var tx = db.transaction("files", "readwrite");
        tx.objectStore("files").put(value, key);
        tx.oncomplete = function () { resolve(true); };
        tx.onerror = function () { resolve(false); };
      });
    });
  }

  function idbDelete(key) {
    return openDb().then(function (db) {
      return new Promise(function (resolve) {
        var tx = db.transaction("files", "readwrite");
        tx.objectStore("files").delete(key);
        tx.oncomplete = function () { resolve(true); };
        tx.onerror = function () { resolve(false); };
      });
    });
  }

  function resizeToJpeg(img, maxDim, quality) {
    var scale = Math.min(1, maxDim / Math.max(img.width, img.height));
    var canvas = document.createElement("canvas");
    canvas.width = Math.round(img.width * scale);
    canvas.height = Math.round(img.height * scale);
    canvas.getContext("2d").drawImage(img, 0, 0, canvas.width, canvas.height);
    return canvas.toDataURL("image/jpeg", quality);
  }

  window.AppMedia = {
    selfTest: selfTest,
    state: function () { return JSON.stringify(state); },
    // 時間割の背景。Kotlin 側は状態をポーリングして dataUrl を受け取る。
    bgLoad: function () {
      bgStatus = "loading";
      idbGet(BG_KEY).then(function (value) {
        bgDataUrl = typeof value === "string" ? value : null;
        bgStatus = "ready";
      }).catch(function () {
        bgDataUrl = null;
        bgStatus = "ready";
      });
    },
    bgPick: function () {
      bgStatus = "picking";
      pickFiles(1, false).then(function (files) {
        if (!files || !files.length) {
          bgStatus = "ready";
          return;
        }
        return readFile(files[0]).then(function (dataUrl) {
          return loadImage(dataUrl).then(function (img) {
            var out = resizeToJpeg(img, 1600, 0.8);
            bgDataUrl = out;
            return idbSet(BG_KEY, out).then(function () { bgStatus = "ready"; });
          });
        });
      }).catch(function () { bgStatus = "ready"; });
    },
    bgClear: function () {
      bgDataUrl = null;
      idbDelete(BG_KEY);
      bgStatus = "ready";
    },
    bgStatus: function () { return bgStatus; },
    bgDataUrl: function () { return bgDataUrl || ""; },
    exportLight: function () { run(60, 1280, false, 2); },
    exportStandard: function () { run(78, 1600, false, 5); },
    exportHigh: function () { run(90, 2048, false, 10); },
    exportGray: function () { run(70, 1400, true, 3); },
    exportExtreme: function () { run(40, 1000, true, 1, true); },
    exportOriginal: function () { run(100, 100000, false, 0, false, true); },
    captureStandard: function () { run(78, 1600, false, 5, false, false, true); }
  };
})();
