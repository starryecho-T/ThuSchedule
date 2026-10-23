(function () {
    'use strict';
    // 在已登录 INFO 门户的页面上下文里，用 JSONP 分批读取本科生教学日历。
    // 接口原生支持 jsoncallback，跨域请求会自动携带 info.tsinghua.edu.cn 会话 Cookie。
    var FIRST_DAY = '__FIRST_DAY__';
    var TOTAL_WEEKS = parseInt('__TOTAL_WEEKS__', 10);
    var GROUP = 3;          // 每批周数，与社区实现的分批口径一致
    var TIMEOUT_MS = 10000; // 单批超时
    var BASE = 'https://info.tsinghua.edu.cn/jxmh_out.do?m=bks_jxrl_all&p_start_date=';

    var state = { done: false, records: [], error: null };
    try { window.__thuJxrlState = state; } catch (ignored) { return; }

    function pad(n) { return (n < 10 ? '0' : '') + n; }
    function fmt(d) { return d.getFullYear() + pad(d.getMonth() + 1) + pad(d.getDate()); }
    function addDays(d, days) { var x = new Date(d.getTime()); x.setDate(x.getDate() + days); return x; }

    var parts = FIRST_DAY.split('-');
    var monday = new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]));

    function finish(error) {
        state.done = true;
        state.error = error || null;
    }

    function fetchBatch(start, end, tag) {
        return new Promise(function (resolve, reject) {
            var name = '__thuJxrlCb' + tag;
            var script = document.createElement('script');
            var timer = setTimeout(function () { cleanup(); reject(new Error('timeout')); }, TIMEOUT_MS);
            function cleanup() {
                clearTimeout(timer);
                try { delete window[name]; } catch (e) { window[name] = undefined; }
                if (script.parentNode) script.parentNode.removeChild(script);
            }
            window[name] = function (data) {
                cleanup();
                if (Object.prototype.toString.call(data) !== '[object Array]') {
                    reject(new Error('payload'));
                    return;
                }
                resolve(data);
            };
            script.onerror = function () { cleanup(); reject(new Error('network')); };
            script.src = BASE + fmt(start) + '&p_end_date=' + fmt(end) + '&jsoncallback=' + name;
            document.head.appendChild(script);
        });
    }

    var chain = Promise.resolve();
    var tag = 0;
    for (var week = 1; week <= TOTAL_WEEKS; week += GROUP) {
        (function (w) {
            var start = addDays(monday, (w - 1) * 7);
            var last = Math.min(w + GROUP - 1, TOTAL_WEEKS);
            var end = addDays(monday, last * 7 - 1);
            chain = chain.then(function () {
                return fetchBatch(start, end, tag++).then(function (rows) {
                    for (var i = 0; i < rows.length; i++) state.records.push(rows[i]);
                });
            });
        })(week);
    }
    chain.then(
        function () { finish(null); },
        function (error) { finish(String(error && error.message || error)); }
    );
})();
