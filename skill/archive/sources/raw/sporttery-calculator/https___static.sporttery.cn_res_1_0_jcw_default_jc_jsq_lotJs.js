var curPool, curData = [], filteAry, isShowOddsChg, oddsName, oddsIndex, selAry = new Object(), selAryLen = 0, times = 1, listTop = -1, loadedOk = false, dAry = [], tAry = [], overDetail = false, min_maxAry,openDetail=false,blurTimeOut;
var optionAry = [[], [], [["2x1", "2"]], [["3x1", "3"], ["3x3", "2"], ["3x4", "23"]], [["4x1", "4"], ["4x4", "3"], ["4x5", "34"], ["4x6", "2"], ["4x11", "234"]], [["5x1", "5"], ["5x5", "4"], ["5x6", "45"], ["5x10", "2"], ["5x16", "345"], ["5x20", "23"], ["5x26", "2345"]], [["6x1", "6"], ["6x6", "5"], ["6x7", "56"], ["6x15", "2"], ["6x20", "3"], ["6x22", "456"], ["6x35", "23"], ["6x42", "3456"], ["6x50", "234"], ["6x57", "23456"]], [["7x1", "7"], ["7x7", "6"], ["7x8", "67"], ["7x21", "5"], ["7x35", "4"], ["7x120", "234567"]], [["8x1", "8"], ["8x8", "7"], ["8x9", "87"], ["8x28", "6"], ["8x56", "5"], ["8x70", "4"], ["8x247", "2345678"]]];
var lotFunc = new Object();
var anns = {};
var limitLen = { "hhad": 8, "had": 8, "crs": 4, "ttg": 6, "hafu": 4, "mnl": 8, "hdc": 8, "wnm": 4, "hilo": 8 };
var fLimitLen = 8;
var curObj;
var LetterAry = { "a": 10, "b": 11, "c": 12, "d": 13, "e": 14, "f": 15, "g": 16, "h": 17, "i": 18, "j": 19, "k": 20, "l": 21, "m": 22, "n": 23, "o": 24, "p": 25, "q": 26, "r": 27, "s": 28, "t": 29, "u": 30, "v": 31, "w": 32, "x": 33, "y": 34, "z": 35 }
var curHeaderObj;
var noMatchShow='<tr><td><div class="m-czNo"><img src="//static.sporttery.cn/res_1_0/jcw/images/ico_game_no.png"/></div></td></tr>';

//add 2019-1-23
var maxTimes = Number(50);//最大倍数
var Limit = Number(6000);

lotFunc.getTrendStr = function(value) {
    if (value > 0) return "Up";
    else if (value < 0) return "Down";
    else return "Keep";
}
lotFunc.getInterface = function() {
    $("#times").val("1");
    $("#optionTip").width("auto");
    //
    filteAry = { date: [], league: [] };
    selAry = new Object();
    selAryLen = 0;
    times = 1;
    lotFunc.calculate();
    //
    $("#hadCkb").prop("checked", true);
    $("#hhadCkb").prop("checked", true);
    $("#floatCkb").prop("checked", false);
    $("#hideCount").html("0");
    //
    $("#mainTbl").html("");
    $("#filterTbl").hide();

    init();
    //测试环境
    setTimeout(dataTransferClass.getJsqMatchDate((curPool.indexOf("had") != -1) ? curPool + ",had" : curPool),50);
};
lotFunc.getReferDataI = function(index) {
    // 对铺完的数据进行斑马线处理
    lotFunc.setZebraCrossing()
    switch (index) {
        case 1:
           // $.getScript("//i.sporttery.cn/odds_calculator/get_bookmarker_odds?i_format=json&sportscode=FB&i_callback=getReferData", null);
            break;
        case 0:
            dataTransferClass.getSupportDate();

            break;
    }
};
// 数据进行斑马线
lotFunc.setZebraCrossing = function (){
    $("#mainTbl tr.listTr").each(function(index) {
        $(this).css('background','#F8F8F8')
        if(index % 2 ==1){
            $(this).css('background','#FDFDFD')
            // $(this).css('background','green')
        }
    });
}
lotFunc.getOddsChgI = function(pool, id) {
    $("#oddsChgDiv").html("<div style='padding:10px;'>&nbsp;&nbsp;waiting...</div>");
    if (pool == "had" || pool == "hhad") {
        dataTransferClass.getOddsHistoryDate(pool, id);
    } else {
        dataTransferClass.getOddsHistoryDate(pool, id);
    }
}
lotFunc.getNoticeI = function() {
    //$.getScript("//i.sporttery.cn/bulletin_list/get_bulletin_title?i_callback=setAnn", null); ////static.sporttery.cn/data/ann.js
}
lotFunc.getVoteI = function(mid, pool, value) {
    dataTransferClass.collectSupportDate(mid, pool, value);
}

lotFunc.getWeek = function(tmpDate) {
    var nd = new Date(tmpDate.replace(/-/g, "/"));
    switch (nd.getDay()) {
        case 0:
            return { num: 0, cn: "日" };
            break;
        case 1:
            return { num: 1, cn: "一" };
            break;
        case 2:
            return { num: 2, cn: "二" };
            break;
        case 3:
            return { num: 3, cn: "三" };
            break;
        case 4:
            return { num: 4, cn: "四" };
            break;
        case 5:
            return { num: 5, cn: "五" };
            break;
        case 6:
            return { num: 6, cn: "六" };
            break;
        default:
            return { num: "?", cn: "?" };
            break;
    }
}
lotFunc.getCombinAryByNum = function(arr, num) {
    var r = [];
    (function f(t, a, n) {
        if (n == 0) return r.push(t);
        for (var i = 0, l = a.length; i <= l - n; i++) {
            f(t.concat(a[i]), a.slice(i + 1), n - 1);
        }
    })([], arr, num);
    return r;
}
lotFunc.getJsonObjCount = function(obj) {
    var l = 0;
    for (var i in obj) {
        l++;
    }
    return l;
}
/*四舍六入五成双*/
lotFunc.rundFunc = function(data, m) {
    var dt = data.toFixed(8).toString();
    if (dt.indexOf('万') > 0) return data; //如果数据中在万以上，就不处理
    if (dt.indexOf('.') < 0) { //如果没有小数点呢？
        pos = dt.length;
    } else {
        pos = dt.indexOf('.') + 3;
    }
    var key = dt.charAt(pos);
    var vals = '';
    if (key < 5) {
        vals = dt.substr(0, pos);
    } else if (key > 5) {
        vals = parseFloat(dt.substr(0, pos)) + 0.01;
    } else {
        if (dt.charAt(pos - 1) % 2) {
            vals = parseFloat(dt.substr(0, pos)) + 0.01;
        } else {
            vals = parseFloat(dt.substr(0, pos));
        }
    }
    //vals已经是2位小数，使用toFixed(2)使之补零操作
    return (parseFloat(vals) * m).toFixed(2);
}
/*function(data, m) {
 //if (data.toString().indexOf('万') > 0) return data; //如果数据中在万以上，不处理
 var dt = data.toFixed(8).toString();
 var pos = dt.indexOf('.') + 3;
 var key = parseInt(dt.charAt(pos));
 var vals = '';
 if (key < 5) {
 vals = dt.substr(0, pos);
 } else if (key > 5) {
 vals = (parseFloat(dt.substr(0, pos)) + 0.01).toString();
 } else {
 if (parseInt(dt.charAt(pos + 1)) > 0) {
 vals = (parseFloat(dt.substr(0, pos)) + 0.01).toString();
 } else if (parseInt(dt.charAt(pos - 1)) % 2) {
 vals = (parseFloat(dt.substr(0, pos)) + 0.01).toString();
 } else {
 vals = parseFloat(dt.substr(0, pos)).toString();
 }
 }
 return Number((Number(vals) * m).toFixed(2));
 }*/
lotFunc.getHideCount = function() {
    var len = $("#mainTbl tr.listTr:hidden").length;
    $("#hideCount").html(len);
    return len;
}
lotFunc.updateDDateState = function() {
    //判断一天显示“隐藏/显示”
    var bDataObj = $("#mainTbl td.bDateTd");
    for (var i = 0; i < bDataObj.length; i++) {
        var tmpObj = bDataObj.eq(i);
        var bindex = tmpObj.attr("bindex");
        var chkObj = $("#dFilterList input:eq(" + bindex + ")");
        if ($("#mainTbl tr.listTr[bindex=" + bindex + "]:visible").length == 0) {
            tmpObj.find(".bDateHide").html("[显示]");
            chkObj.prop("checked", false);
        } else {
            tmpObj.find(".bDateHide").html("[隐藏]");
            chkObj.prop("checked", true);
        }
    }
}
lotFunc.updateFilterMatch = function() {
    var filterObj = $("#mFilterList input");
    filterObj.prop("checked", false);
    var obj = $("#mainTbl tr.listTr:visible");
    for (var i = 0; i < obj.length; i++) {
        var lIndex = obj.eq(i).attr("lindex");
        filterObj.eq(lIndex).prop("checked", true);
    }
    if ($("#mFilterList input:checked").length == filterObj.length - 1) {
        filterObj.eq(0).prop("checked", true);
    }
}
lotFunc.mySort = function(myAry, key, st, child) {
    return myAry.sort(function(a, b) {
        if (key == "num") {
            if (st == 'asc') {
                return Number(a[key].substr(2)) - Number(b[key].substr(2));
            } else if (st == 'desc') {
                return Number(b[key].substr(2)) - Number(a[key].substr(2));
            }
        } else if (child != undefined) {
            if (st == 'asc') {
                if (a[key] == undefined) {
                    return -1;
                }
                if (b[key] == undefined) {
                    return 1;
                }
                return a[key][child] - b[key][child];
            } else if (st == 'desc') {
                if (a[key] == undefined) {
                    return 1;
                }
                if (b[key] == undefined) {
                    return -1;
                }
                return b[key][child] - a[key][child];
            }
        }
    });
}
lotFunc.sortByOdds = function(sortKey, oddsStr, orderStr) {
    if ($("#singlePass").prop("checked") == true){
        $("#mFilterBtn").removeClass("grey");
        $("#singlePass").prop("checked", false);
    } 
    var headerId = oddsStr + "Header";
    var seqStr = "desc";
    if (orderStr != undefined) {
        seqStr = orderStr;
    } else {
        var seq = $("#" + headerId).hasClass("arrow_re");
        if (seq) {
            seqStr = "asc";
        }
    }
    for (var i = 0; i < curData.length; i++) {
        lotFunc.mySort(curData[i], sortKey, seqStr, oddsStr);
    }
    initData();
    //
    if (seqStr == "desc") {
        $("#" + headerId).addClass("arrow_re");
    } else {
        $("#" + headerId).removeClass("arrow_re");
    }
    //
    $("#sortList").offset({ left: 0, top: 0 });
    $("#sortList").hide();
}
lotFunc.timesChg = function(num) {
    if (num == undefined) num = 0;
    var tValue = Math.round(Number($("#times").val()));
    tValue = tValue + num;
    if (isNaN(tValue)) tValue = 1;
    else if (tValue < 1) tValue = 1;
    else if (tValue > maxTimes) tValue = maxTimes;
    times = tValue;
    $("#times").val(tValue);
    lotFunc.calculate();
}
lotFunc.checkToolChk = function() {
    if ($("#hadCkb").prop("checked") == false) {
        $("#mainTbl div.hadOdds span.oddsItem").addClass("oddsEffect");
    } else if ($("#hhadCkb").prop("checked") == false) {
        $("#mainTbl div.hhadOdds span.oddsItem").addClass("oddsEffect");
    }
}
lotFunc.initOK = function() {
    //几场比赛
    var bDateObj = $("#mainTbl td.bDateTd");
    for (var i = 0; i < bDateObj.length; i++) {
        var tmpObj = bDateObj.eq(i);
        tmpObj.find("label").html($("#mainTbl tr.listTr[bIndex=" + tmpObj.attr("bIndex") + "]").length);
    }
    //更新时间
    //var dateObj = new Date();
    //$("#updateTime").html(dateObj.getMonth() + "-" + dateObj.getDate() + " " + dateObj.getHours() + ":" + dateObj.getMinutes() + ":" + dateObj.getSeconds());
    //填充筛选选项
    var filterStr = "<input type='checkbox' /> 全部";
    for (var i = 0; i < filteAry.date.length; i++) {
        filterStr += "<input type='checkbox' /> 周" + filteAry.date[i] + " ";
    }
    $("#dFilterList").html(filterStr);
    filterStr = "<input type='checkbox' /> <label style='width:100px;display:inline-block;'>全部</label><br>";
    for (var i = 0; i < filteAry.league.length; i++) {
        if (i > 0 && i % 3 == 0) filterStr += "<br>";
        filterStr += "<input type='checkbox' /> <label style='width:100px;display:inline-block;'>" + filteAry.league[i] + "</label> ";
    }
    $("#mFilterList").html(filterStr);
    //
    $("#filterTbl input").prop("checked", true);
    $("#filterResize").css("width", $("#filterTbl").width() - 68);
}
lotFunc.calculate = function() {
    //
    lotFunc.updateOptionPan(false);
    var combinOptionStr = lotFunc.getCombinOptStr();
    //
    updateSelDetail();
    //
    $("#selCount").html(selAryLen);
    //
    $("#consume").text("");
    $("#bonus").text("");

    //单关计算
    var singleBonus = 0;
    var singleCount = 0;
    if ($("#optionList input:eq(0)").prop("checked") == true) {
        for (var key in selAry) {
            var bValue = 0;
            if (selAry[key].single) {
                var len = selAry[key].odds.length;
                for (var i = 0; i < len; i++) {
                    if (bValue < selAry[key].odds[i] * 2 * times) {
                        bValue = selAry[key].odds[i] * 2 * times;
                    }
                    if (selAry[key].odds[i] != "") singleCount += 2;
                }
            }
            singleBonus += bValue;
        }

        if ($("#optionList input:checked").length == 1) {
            $("#consume").text(singleCount * times);
            $("#bonus").text(singleBonus.toFixed(2));
            lotFunc.showTaxDateNo()
            return;
        }
    }

    //
    dAry = [];
    tAry = [];
    var isFree = ($("#optionHeader :checked").attr("index") == 1);
    for (var key in selAry) {
        if (isFree) {
            tAry.push(selAry[key]);
        } else {
            if (selAry[key].isDan == undefined) {
                tAry.push(selAry[key]);
            } else {
                dAry.push(selAry[key]);
            }
        }
    }

    //胆中最大最小
    var dMaxAry = [];
    var dCountAry = [];
    var dMinAry = [];
    for (var i = 0; i < dAry.length; i++) {
        var minValue = 10000000;
        dCountAry.push(lotFunc.getOddsLen(dAry[i].odds));
        var maxValue = 1;
        for (var j = 0; j < dAry[i].odds.length; j++) {
            if (dAry[i].odds[j] == "") {
                continue;
            }
            var oddsValue = Number(dAry[i].odds[j]);
            if (oddsValue > maxValue) {
                maxValue = oddsValue;
            }
            if (oddsValue < minValue) {
                minValue = oddsValue;
            }
        }
        dMinAry.push(minValue);
        dMaxAry.push(maxValue);
    }

    //查找拖最大值和最小值
    var calMaxAry = [];
    var calMinAry = [];
    var multiCountAry = [];
    for (var i = 0; i < tAry.length; i++) {
        multiCountAry.push(lotFunc.getOddsLen(tAry[i].odds));
        var minValue = 10000000;
        var maxValue = 1;
        for (var j = 0; j < tAry[i].odds.length; j++) {
            if (tAry[i].odds[j] == "") {
                continue;
            }
            var oddsValue = Number(tAry[i].odds[j]);
            if (oddsValue > maxValue) {
                maxValue = oddsValue;
            }
            if (oddsValue < minValue) {
                minValue = oddsValue;
            }
        }
        calMinAry.push(minValue);
        calMaxAry.push(maxValue);
    }

    var maxBonus = 0;
    var mnCount = 0;

    var obj = lotFunc.getBonus(combinOptionStr, calMaxAry, dMaxAry, multiCountAry, dCountAry);

    maxBonus = obj.maxBonus;
    minBonus = obj.minBonus;
    mnCount = obj.mnCount;
    //add max limit
    var tmoney = mnCount * 2 * times + singleCount * times;
    var finalBonus = (maxBonus + singleBonus).toFixed(2);
    $("#consume").text(tmoney);
    $("#bonus").text(finalBonus);
    if(tmoney>=Limit){
        alert("温馨提示：当前投注金额已超"+Limit+"元，请理性购彩。");
    }

   lotFunc.showTaxDateNo();
    //
    min_maxAry = { "dMax": dMaxAry, "dMin": dMinAry, "tMax": calMaxAry.sort(), "tMin": calMinAry.sort() };
}

lotFunc.checkDanCount = function(optStr, dLen) {
    for (var i = 0; i < optStr.length; i++) {
        var tmpStr = optStr.charAt(i);
        if (Number(tmpStr) < dLen) {
            alert("胆的数量多于当前过关选项中的过关方式");
            return false;
        }
    }
    return true;
}

lotFunc.getOddsLen = function(ary) {
    var len = 0;
    for (var i = 0; i < ary.length; i++) {
        if (ary[i] != "") {
            len++;
        }
    }
    return len;
}

lotFunc.getCombinOptStr = function() {
    var combinOptionStr = "";

    if ($("#optionHeader input").attr("disabled") != undefined) {
        return "";
    }
    if ($("#optionHeader :checked").attr("index") == 0) {  //普通过关
        var selOptionAry = optionAry[lotFunc.getSelMatchCount()];
        if (selOptionAry == undefined) return "";
        var oAry = selOptionAry[$("#optionList input:gt(0):checked").index("#optionList input") - 1];
        if (oAry != undefined) combinOptionStr = oAry[1];
    }
    else { //多选过关
        var optionSelObj = $("#optionList input:gt(0):checked");
        for (var i = 0; i < optionSelObj.length; i++) {
            var optionStr = optionSelObj.eq(i).attr("optStr");
            if (optionStr != undefined) combinOptionStr += optionStr;
        }
    }
    return combinOptionStr;
}

lotFunc.getBonus = function(optionsStr, oddMaxAry, oddDanAry, multiCountAry, dCountAry) {
    //var len = oddMaxAry.length;
    //oddMaxAry = oddMaxAry.slice(len - goalCount);
    //oddMinAry = oddMinAry.slice(0, goalCount);

    var maxBonus = 0;
    //var minBonus = 0;
    var mnCount = 0;

    for (var i = 0; i < optionsStr.length; i++) {
        var len = Number(optionsStr.charAt(i)) - oddDanAry.length;
        if (len < 0) continue;
        var resultAry = lotFunc.getCombinAryByNum(oddMaxAry, len);
        if (multiCountAry != null) {
            var tmpAry = lotFunc.getCombinAryByNum(multiCountAry, len);
            for (var m = 0; m < tmpAry.length; m++) {
                tmpAry[m] = tmpAry[m].join("*");
                if (tmpAry[m] == "") tmpAry[m] = 1;
                if (dCountAry.length > 0) {
                    tmpAry[m] += "*" + dCountAry.join("*");
                }
            }
            mnCount += eval(tmpAry.join("+"));
        }
        for (var j = 0; j < resultAry.length; j++) {
            var mergeAry = resultAry[j].concat(oddDanAry);
            var tmpBonus = eval(mergeAry.join("*"));
            /*maxBonus += lotFunc.oddsMNLimit(tmpBonus, selAryLen);*/
            maxBonus += lotFunc.oddsMNLimit(tmpBonus,Number(optionsStr.charAt(i)));
        }
    }
    return { maxBonus: Math.round(maxBonus * 100) / 100, mnCount: mnCount };
}

lotFunc.getMMBonus = function(optionsStr, ary, goalCount) {

    var oddMaxAry = ary.tMax.slice(ary.tMax.length - goalCount + ary.dMax.length);
    var oddMinAry = ary.tMin.slice(0, goalCount - ary.dMax.length);

    var maxBonus = 0;
    var minBonus = 0;

    for (var i = 0; i < optionsStr.length; i++) {
        var optNum = Number(optionsStr.charAt(i));
        if (optNum > goalCount) continue;
        var len = optNum - ary.dMax.length;
        if (len < 0) continue;

        var resultAry = lotFunc.getCombinAryByNum(oddMaxAry, len);
        for (var j = 0; j < resultAry.length; j++) {
            var mergeAry = resultAry[j].concat(ary.dMax);
            var tmpBonus = eval(mergeAry.join("*"));
            maxBonus += lotFunc.oddsMNLimit(tmpBonus, optNum);
        }

        resultAry = lotFunc.getCombinAryByNum(oddMinAry, len);
        for (var j = 0; j < resultAry.length; j++) {
            var mergeAry = resultAry[j].concat(ary.dMin);
            var tmpBonus = eval(mergeAry.join("*"));
            minBonus += lotFunc.oddsMNLimit(tmpBonus, optNum);
        }
    }
    return { maxBonus: Math.round(maxBonus * 100) / 100, minBonus: Math.round(minBonus * 100) / 100, goalCount: goalCount };
}

lotFunc.oddsMNLimit = function(bonus, len) {
    bonus = lotFunc.rundFunc(bonus * 2, 1);
    //奖金限制
    switch (len) {
        case 2:
        case 3:
            if (bonus > 200000) bonus = 200000;
            break;
        case 4:
        case 5:
            if (bonus > 500000) bonus = 500000;
            break;
        case 6:
        case 7:
        case 8:
            if (bonus > 1000000) bonus = 1000000;
            break;
    }
    return Number((bonus * times).toFixed(2));
}
//将选择结果显示到奖金列表区
lotFunc.applaySel = function() {
    for (var key in selAry) {
        var tmpObj = selAry[key];
        var listObj = $("#list_" + key.split("@")[0]);
        var pool = tmpObj.pool;
        tmpObj.dataIndex = listObj.attr("dataindex");
        for (var i = 0; i < tmpObj.odds.length; i++) {
            if (tmpObj.odds[i] != "") {
                //odds栏
                listObj.find("." + pool + "Odds .oddsItem:eq(" + i + ")").addClass("oddsClk");
                //信息栏
                if (curPool.indexOf("hhad") != -1) listObj.find("." + pool + "U span:eq(" + i + ")").addClass("uOddsSel");
            }
        }
    }
}
lotFunc.getSelMatchCount = function() {
    var tmpAry = [];
    for (var key in selAry) {
        tmpAry.push(key.split("@")[0]);
    }
    tmpAry.sort();
    var len = selAryLen;
    if (tmpAry.length > 1) {
        for (var i = 0; i < tmpAry.length - 1; i++) {
            if (tmpAry[i] == tmpAry[i + 1]) {
                len--;
            }
        }
    }
    return len;
}
lotFunc.optionDis = function() {
    var tipStr = "";
    //如果其中有串关限制，串关选项变灰
    var tmpObj = new Object();
    var hasMulti = false;
    for (var key in selAry) {
        if (tmpObj[key.split("@")[0]] != undefined) {
            hasMulti = true;
            break;
        } else {
            tmpObj[key.split("@")[0]] = 1;
        }
    }
    var selCount = lotFunc.getSelMatchCount();

    if (hasMulti || ($("#optionHeader :checked").attr("index") == 0 && selCount > limitLen[curPool]) || ($("#optionHeader :checked").attr("index") == 1 && selCount > fLimitLen)) {
        //清除选择项
        $("#optionList input:gt(0)").prop("checked",false);

        $("#optionList input:gt(0)").attr("disabled", true);
        if ($("#optionHeader :checked").attr("index") == 0) {
            if (selCount > limitLen[curPool]) {
                var addStr = "";
                if(curPool == "ttg" || curPool == "crs" || curPool == "hafu" || curPool == "wnm"){ addStr = "或自由过关"; }
                tipStr = " 超过" + limitLen[curPool] + "场只能选择单关"+ addStr +"进行计算！";
            }
        } else if ($("#optionHeader :checked").attr("index") == 1) {
            if (selCount > fLimitLen) {
                tipStr = " 超过" + fLimitLen + "场只能选择单关进行计算！";
            }
        }
        if (hasMulti) {
            tipStr = " 同时选一场比赛的两个单关游戏，只能计算单关！";
        }
        $("#optionList input:gt(0)").attr("disabled", true);
        //$("#optionHeader input").attr("disabled", true);
    } else {
        $("#optionList input:gt(0)").removeAttr("disabled");
        $("#optionHeader input").removeAttr("disabled");
    }
    $("#optionTip").html(tipStr);
    if (tipStr == "") {
        $("#optionTip").width("auto");
    }
}
//
lotFunc.updateOptionPan = function(update) {

    /*
     var len = selAryLen;
     var isSingleSel = ($("#optionList input:eq(0)").attr("checked") == "checked");
     if (isSingleSel) {
     //判断多少场比赛
     len = lotFunc.getSelMatchCount();
     }
     */
    var isSingleSel = ($("#optionList input:eq(0)").prop("checked") == true);
    len = lotFunc.getSelMatchCount();
    if (len > limitLen[curPool]) {
        len = limitLen[curPool];
    }
    //
    lotFunc.optionDis();


    //检测有多少场单关
    var singleCount = 0;
    for (var key in selAry) {
        if (selAry[key].single) {
            singleCount++;
        }
    }

    if (Number($("#optionList").attr("count")) == len) {
        if (!update) {
            $("#sCount").text(singleCount);
            return;
        }
    } else {
        $("#optionList").attr("count", len);
    }

    var checkIndex = $("#optionList input:gt(0):checked").index("#optionList input") - 1;

    var optionStr = "<input" + ((isSingleSel) ? " checked" : "") + " type='checkbox'>单关(<span id='sCount'>" + singleCount + "</span>场)";
    if (len == 0) optionStr = "";

    //胜平负让球胜平负同时选len-1
    if (curPool == "hhad") {
        var tmpObj = new Object();
        for (var key in selAry) {
            tmpObj[key.split("@")[0]] = 1;
        }
        var tmpLen = 0;
        for (var key in tmpObj) {
            tmpLen++;
        }
        len = tmpLen;
    }

    if ($("#optionHeader :checked").attr("index") == 0) {
        //普通过关
        if (len > 1) {
            for (var i = 0; i < optionAry[len].length; i++) {
                var checkStr = "";
                //if (i == 0 && !isSingleSel) checkStr = " checked";
                optionStr += "<input type='checkbox' " + checkStr + " />" + optionAry[len][i][0];
            }
        }
    } else {
        //多选过关
        if (len > 1) {
            for (var i = 2; i <= len; i++) {
                var checkStr = "";
                //if (i == 2 && !isSingleSel) checkStr = " checked";
                optionStr += "<input type='checkbox'" + checkStr + " optStr='" + i + "' />" + i + "关";
            }
        }
    }
    $("#optionList").html(optionStr);

    if (checkIndex >= 0) {
        $("#optionList input:eq(1)").prop("checked", true);
    }
    //没有单关单关选项不可选
    if (singleCount == 0) {
        $("#optionList input:eq(0)").attr("disabled", true);
    } else {
        $("#optionList input:eq(0)").removeAttr("disabled");
    }
    //
    //如果其中有串关限制，串关选项变灰
    lotFunc.optionDis();
}
//
lotFunc.scrollFunc = function() {
    var sTop = $(document).scrollTop();
    var sel_panObj = $("#sel_pan");
    var mainTblObj = $("#mainTbl");
    var curTop = sTop + $(window).height() - sel_panObj.height();
    if (curTop > mainTblObj.offset().top + mainTblObj.height()) {
        sel_panObj.css("position", "relative");
        if (lotFunc.isIE6()) {
            sel_panObj.offset({ top: mainTblObj.offset().top + mainTblObj.height() });
        }
    } else {
        if (lotFunc.isIE6()) {
            sel_panObj.css("position", "relative");
            sel_panObj.offset({ top: curTop });
        } else {
            sel_panObj.css("position", "fixed");
            sel_panObj.css("bottom", "0px");
        }
    }
    //
    if (sTop > listTop) {
        if (lotFunc.isIE6()) {
            $("#headerTr").css("position", "relative");
            $("#headerTr").offset({ top: sTop });
        } else {
            //$("#headerTr").css("position", "fixed");
            $("#headerTr").css("top", "0px");
        }
    } else {
        $("#headerTr").css("position", "relative");
        if (lotFunc.isIE6()) {
            $("#headerTr").offset({ top: listTop });
        }
    }
}
//
lotFunc.resetTdSize = function() {
    var mTdObj = $("#mainTbl .listTr:visible").eq(0).find("td");
    var hTdObj = $("#headerTr td");
    for (var i = 0; i < hTdObj.length; i++) {
        mTdObj.eq(i).width(hTdObj.eq(i).width());
    }
    //
    lotFunc.scrollFunc();
}
//
lotFunc.getMNDetail = function(ary, optionStr) {
    var infoAry = lotFunc.getCombinByIndex(ary.length, optionStr);
    var returnAry = [];
    var radix = 32;
    for (var i = 0; i < infoAry.length; i++) {
        var tmpAry = infoAry[i];
        var oddsLenAry = [];
        for (var j = 0; j < tmpAry.length; j++) {
            var len = lotFunc.getOddsLen(ary[tmpAry[j]].odds);
            oddsLenAry.push(len.toString(radix));
        }
        var lenStr = oddsLenAry.join("");
        var startNum = parseInt((Math.pow(2, tmpAry.length) - 1).toString(2), radix);
        var endNum = parseInt(lenStr, radix);
        for (var j = startNum; j <= endNum; j++) {
            var tmpStr = j.toString(radix);
            var isContinue = false;
            for (var m = 0; m < tmpStr.length; m++) {
                if (tmpStr.charAt(m) > lenStr.charAt(m)) {
                    isContinue = true;
                    var str = tmpStr.substr(0, m);
                    for (var n = m; n < tmpStr.length; n++) {
                        str += "1";
                    }
                    num = parseInt(str, radix);
                    num += Math.pow(radix, tmpStr.length - m) - 1;
                    j = num;
                    break;
                }
            }
            if (isContinue) continue;
            returnAry.push([tmpStr, i]);
        }
    }
    return { combinAry: infoAry, indexAry: returnAry };
}
//
lotFunc.getCombinByIndex = function(len, optionStr) {
    var parse2Num = Math.pow(2, len) - 1;
    var infoAry = [];
    var tmpAry = [];
    for (var m = 0; m < optionStr.length; m++) {
        for (var i = 1; i <= parse2Num; i++) {
            var radix2Str = i.toString(2);
            var addNum = 0;
            var tmpAry = [];
            for (var j = radix2Str.length - 1; j >= 0; j--) {
                var bitValue = Number(radix2Str.charAt(j));
                addNum += bitValue;
                if (bitValue > 0) {
                    var aryIndex = radix2Str.length - 1 - j;
                    tmpAry.push(aryIndex);
                }
            }
            if (addNum == Number(optionStr.charAt(m))) {
                infoAry.push(tmpAry);
            }
        }
    }
    return infoAry;
}
//
lotFunc.ready = function() {
    if (! -[1, ]) document.execCommand("BackgroundImageCache", false, true);
    //
    if (lotFunc.has_flash()) {
    } else {
        $("#copytoboard").hide();
    }
    lotFunc.getJsqConfigData();
    //
    //公告
    lotFunc.getNoticeI();
    //
    $("#mainTbl").on("click",".bDateHide", function() {
        //同步到filter面板
        var bIndex = $(this).closest("td").attr("bindex");
        var chkObj = $("#dFilterList input:eq(" + bIndex + ")");
        //
        if ($(this).html() == "[隐藏]") {
            $("#mainTbl tr.listTr[bIndex=" + $(this).parent().attr("bIndex") + "]").hide();
            $(this).html("[显示]");
            chkObj.prop("checked", false);
        } else {
            $("#mainTbl tr.listTr[bIndex=" + $(this).parent().attr("bIndex") + "]").show();
            $(this).html("[隐藏]");
            chkObj.prop("checked", true);
        }
        //
        lotFunc.checkCrsShow();
        lotFunc.resetTdSize();
        lotFunc.getHideCount();
    });
    //
    $("#mFilterBtn").click(function() {
        if($(this).hasClass("grey")){
            return false;
        }
        $("#filterTbl").show();
        $("#filterTbl").width("auto");
        $("#filterTbl").offset({ left: $(this).offset().left, top: $(this).offset().top });
        lotFunc.updateFilterMatch();
    });
    //tr行事件
    $("#mainTbl").on("mouseenter","tr.listTr", function() {
        $(this).addClass("listTrOver");
    });
    $("#mainTbl").on("mouseleave","tr.listTr", function() {
        $(this).removeClass("listTrOver");
    });
    //筛选关闭按钮
    $("#filterCloseBtn").click(function() {
        $("#filterTbl").hide();
    });
    $("#filterHeader").click(function() {
        $("#filterTbl").hide();
    }); //筛选 中的checkbox事件
    $("#filterTbl").on("click","input", function() {
        var tmpObj = $(this).closest("td");
        var index = tmpObj.find("input").index($(this));
        var tdId = tmpObj.attr("id");
        var isCheck = Boolean(this.checked);
        if (index == 0) { //全部
            $("#filterTbl input").prop("checked", isCheck);
            if (isCheck) {
                $("#mainTbl tr.listTr").show();
            } else {
                $("#mainTbl tr.listTr").hide();
            }
        } else {
            switch (tdId.substr(0, 1)) {
                case "d": //日期
                    if (isCheck) {
                        $("#mainTbl tr.listTr[bindex=" + index + "]").show();
                    } else {
                        $("#mainTbl tr.listTr[bindex=" + index + "]").hide();
                        $(this).closest("td").find("input:eq(0)").prop("checked", false);
                    }
                    //相应赛事显示
                    $("#mFilterList input").prop("checked", false);
                    $("#mainTbl tr.listTr:visible").each(function() {
                        $("#mFilterList input").eq($(this).attr("lindex")).prop("checked", true);
                    });
                    break;
                case "m": //赛事
                    if (isCheck) {
                        $("#mainTbl tr.listTr[lindex=" + index + "]").show();
                    } else {
                        $("#mainTbl tr.listTr[lindex=" + index + "]").hide();
                        $(this).closest("td").find("input:eq(0)").prop("checked", false);
                        $("#dFilterList").find("input:eq(0)").prop("checked", false);
                    }
                    break;
            }
        }
        lotFunc.checkCrsShow();
        lotFunc.getHideCount();
        lotFunc.updateDDateState();
        lotFunc.resetTdSize();
        if ($(this).closest("td").find("input:checked").length == ($(this).closest("td").find("input").length-1)) {
            $("#filterTbl").find("input").prop("checked", true);
        }
    });
    //排序 code
    $("#codeHeader").on("click", function() {
        if ($("#singlePass").prop("checked") == true){
            $("#mFilterBtn").removeClass("grey");
            $("#singlePass").prop("checked", false);
        } 
        var seq = $(this).hasClass("arrow_re");
        var seqStr = "desc";
        if (seq) {
            seqStr = "asc";
        }
        for (var i = 0; i < curData.length; i++) {
            lotFunc.mySort(curData[i], "num", seqStr);
        }
        //
        initData();
        //
        if (seqStr == "desc") {
            $("#codeHeader").addClass("arrow_re");
        } else {
            $("#codeHeader").removeClass("arrow_re");
        }
        //
        lotFunc.checkToolChk();
        //
        lotFunc.applaySel();
        //重置selAry index

    });
    $("#times").change(function() { //倍数改变
        lotFunc.timesChg();
    });
    $("#subBtn").click(function() { //倍数减
        lotFunc.timesChg(-1);
    });
    $("#addBtn").click(function() { //倍数加
        lotFunc.timesChg(1);
    });
    $("#optionHeader input").on("change", function() { //过关方式
        $("#optionList input").prop("checked", false);
        lotFunc.updateOptionPan(true);
        lotFunc.calculate();
    });
    /*
     $("#optionList input").on("mousedown", function() {
     if ($(this).index() > 0) {
     if (selAryLen > limitLen[curPool]) {
     alert("超过" + limitLen[curPool] + "场");
     $("#selDetailDiv").show();
     updateSelDetail();

     } else {
     if (lotFunc.getSelMatchCount() != selAryLen) {
     alert("一场比赛中，只允许选择一个游戏进行过关");
     if (!$("#selDetailBtn").hasClass("detailBtnClk")) {
     $("#selDetailBtn").click();
     }
     }
     }
     }
     });
     */
    $("#optionList").on("change","input", function() {
        //选择过关选项
        if ($("#optionHeader :checked").attr("index") == 0) {
            //普通过关
            if (this.checked) {
                var index = $(this).index("#optionList input");
                
                if (index > 0) {
                    $("#optionList input:gt(0)").prop("checked", false);
                    $("#optionList input:eq(" + index + ")").prop("checked", true);
                }
            } else {
            }
        }
        //
        //判断胆个数
        if ($(this).prop("checked") == true) {
            if (!lotFunc.checkDanCount(lotFunc.getCombinOptStr(), dAry.length)) {
                $(this).prop("checked",false);
                return;
            }
        }
        //
        lotFunc.calculate();
    });
    $("#selDetailBtn").click(function() { //打开选择详细列表
        if($("#selCount").text() <=0){
            return;
        }
        if (!$(this).hasClass("detailBtnClk")) {
            $("#selDetailDiv").show();
            updateSelDetail();
            $(this).addClass("detailBtnClk");
        } else {
            $(this).removeClass("detailBtnClk");
            $("#selDetailDiv").fadeOut();
        }
    });
    $(document).on("click", "#selDetailClose", function() { //选择详细的×
        $("#selDetailBtn").click();
    });
    $(document).on("click","#clearSel", function() { //删除全部选择
        selAry = new Object();
        selAryLen = 0;
        lotFunc.calculate();
        $("#mainTbl span.oddsClk").removeClass("oddsClk");
        if (curPool.indexOf("had") != "") $("#mainTbl span.uOddsSel").removeClass("uOddsSel"); //右侧
        $("#selDetailDiv").fadeOut();
        $("#selDetailBtn").removeClass("detailBtnClk");
    });
    $("#selDetailTbl").on("click",".delSelLine", function() { //删除一行选择
        var id = $(this).closest("tr").attr("id");
        var num = id.split("_")[1];
        var poolStr = num.split("@")[1];
        if (poolStr == undefined) poolStr = curPool;
        delete selAry[num];
        selAryLen--;
        //删除右侧
        var trObj = $("#" + id.replace("sel", "list").split("@")[0]);
        if (poolStr.indexOf("had") != -1) {
            trObj.find("." + poolStr + "U").find(".uOddsSel").removeClass("uOddsSel");
            trObj.find("." + poolStr + "Odds").find(".oddsItem").removeClass("oddsClk");
        } else {
            trObj.find(".oddsItem").removeClass("oddsClk");
        }
        //
        lotFunc.calculate();
    });
    $("#selDetailTbl td.selItemTd").on("click","span", function() {
        var trObj = $(this).closest("tr");
        var num = trObj.attr("id").split("_")[1];
        var index = Number($(this).attr("index"));
        var listTr = $("#" + trObj.attr("id").replace("sel", "list").split("@")[0]);
        var selObj = selAry[num];
        //selAry
        selObj.odds[index] = "";
        //右侧
        if (curPool == "crs") {
            listTr.find(".oddsItem:eq(" + index + ")").removeClass("oddsClk");
        } else {
            listTr.find("." + selObj.pool + "Odds .oddsItem:eq(" + index + ")").removeClass("oddsClk");
        }
        if (Number(selObj.odds.join("")) == 0) {
            $(this).closest("tr").find(".delSelLine").click();
        }
        //
        lotFunc.calculate();
    });
    //设胆
    $("#selDetailTbl").on("click",".danChk", function() {
        //判断胆个数
        if (this.checked) {
            if (!lotFunc.checkDanCount(lotFunc.getCombinOptStr(), dAry.length + 1)) {
                $(this).prop("checked",false)
                return;
            }
        }

        var key = $(this).closest("tr").attr("id").split("_")[1];
        if (Boolean(this.checked)) {
            selAry[key].isDan = true;
        } else {
            delete selAry[key].isDan;
        }
        //
        lotFunc.calculate();
    });
    //显示详细
    $("#detailBtn").click(function() {
        if ($("#viewDetailDiv").css("display") != "none") {
            return;
        }

        if ($("#consume").text() == "0") return;
        //if (selAryLen < 2 && ) return;

        $("#viewDetailTbl").html("<tr><td>计算中，请稍等...<br>（如果浏览器长时间无反应，请刷新页面）</td></tr>");
        $("#viewDetailDiv").show();

        setTimeout(function () {
            lotFunc.calDetail()
        }, 100);
    });
    //
    $("#detailClose").on("click", function() {
        $("#viewDetailDiv").hide();
    });
    //
    $("#printBtn").on("click", function() {
        $("#viewDetailTbl").printArea();
    });
    //
    $("#viewDetailDiv").mouseover(function() {
        overDetail = true;
    });
    $("#viewDetailDiv").mouseout(function() {
        overDetail = false;
    });
    $("#detailClose").on("blur", function(e) {
        //判断鼠标位置
        if (!overDetail) {
            $("#viewDetailDiv").fadeOut();
        } else {
            setTimeout(function() { $("#detailClose").focus(); }, 500);
        }
    });
    //
    /* $("#uOddsListbox").click(function() {
         if ($("#uOddsSelect:visible").length == 0) {
             clearInterval(blurTimeOut);
             $("#uOddsSelect").show();
             $("#uOddsSelect").offset({ left: $(this).offset().left, top: $(this).offset().top + $(this).height() - 2 });

             if ($("#uOddsSelect a:eq(0)").text() == $(this).text()) {
                 $("#uOddsSelect a:eq(0)").focus();
             } else {
                 $("#uOddsSelect a:eq(1)").focus();
             }

             //$("#uOddsSelect a:eq(0)").focus();

             $(this).css("border", "solid 1px #AAAAAA");
         } else {
             $("#uOddsSelect").hide();
             $("#uOddsSelect a").blur();
             $(this).css("border", "none");
         }
     });*/
    //
    /* $("#uOddsSelect a").blur(function() {
         clearInterval(blurTimeOut);
         blurTimeOut = setTimeout(function() { $("#uOddsSelect").hide(); $("#uOddsListbox").css("border", "none"); }, 300);
     });*/
    /* $("#uOddsSelect a").click(function() {
         $("#uOddsSelect").hide();
         $("#uOddsSelect a").blur();
         $("#uOddsListbox").css("border", "none");
         //
         if ($("#uOddsListbox").text() != $(this).text()) {
             var index = $(this).index() / 2;
             switch (index) {
                 case 1: //百家平均
                     $("#uOddsListbox").html("百家平均<img src='images/listArrow2.gif'>");
                     break;
                 case 0: //投注比例
                     $("#uOddsListbox").html("支持率<img src='images/listArrow2.gif'>");
                     break;
             }
             lotFunc.getReferDataI(index);
         }
     });*/
    //刷新按钮
    $("#updateBtn").click(function() { lotFunc.getInterface() });
    //加载数据
    lotFunc.getInterface();
    //
    $("#divStayTopleft").click(function() {
        $("#headerTr").css("position", "relative");
    });
}
lotFunc.getPoolTypeStr = function(ary0, ary1) {
    var poolStr = "";
    if (curPool.indexOf("had") != -1) {
        if (ary0 != null && ary1 != null) {
            if ((ary0.had || ary1.pool.had) && (ary0.hhad || ary1.pool.hhad)) {
                poolStr = "混合过关";
            } else if ((ary0.had || ary1.pool.had)) {
                poolStr = "胜平负";
            } else {
                poolStr = "让球胜平负";
            }
        } else if (ary0 != null) {
            if (ary0.had && ary0.hhad) {
                poolStr = "混合过关";
            } else if (ary0.had) {
                poolStr = "胜平负";
            } else {
                poolStr = "让球胜平负";
            }
        } else if (ary1 != null) {
            if (ary1.pool.had && ary1.pool.hhad) {
                poolStr = "混合过关";
            } else if (ary1.pool.had) {
                poolStr = "胜平负";
            } else {
                poolStr = "让球胜平负";
            }
        }
    }
    return poolStr;
}
//
lotFunc.checkCrsShow = function() {
    if (curPool == "crs") {
        var obj = $("#mainTbl tr.listTr");
        for (var i = 0; i < obj.length; i++) {
            if (obj.eq(i).css("display") == "none") {
                obj.eq(i).next().hide();
            } else {
                if (obj.eq(i).find("span.folderTd").attr("title") == "展开") {
                    obj.eq(i).next().hide();
                } else {
                    obj.eq(i).next().show();
                }
            }
        }
    }
}
//
lotFunc.autoScroll = function() {
    if ($("#selDetailTbl tr").length > 12) {
        $("#selDetailDiv").height(400);
        $("#selDetailDiv").width("auto");
        $("#selDetailDiv").width($("#selDetailTbl").width() + 20); $("#selDetailDiv").css("overflow-y", "auto"); $("#selDetailDiv").css("overflow", "auto");
    } else {
        $("#selDetailDiv").height("auto");
        $("#selDetailDiv").width("auto");
    }
}
//
lotFunc.calDetail = function() {

    //单关固定 详细
    //<tr><td class='orderTd'></td><td>" + lotFunc.getPoolTypeStr(null, dResultAry[n]) + dAry.length + "串1</td><td>" + (dResultAry[n].str + times + "倍") + "</td><td>" + lotFunc.oddsMNLimit(dResultAry[n].bonus, dAry.length) + "</td></tr>;
    var singleDetailStr = "";
    if ($("#optionList input:eq(0)").prop("checked") == true) {
        for (var key in selAry) {
            if (selAry[key].single) {
                for (var i = 0; i < selAry[key].odds.length; i++) {
                    if (selAry[key].odds[i] != "") {
                        var tmpData = curData[selAry[key].dataIndex.split("_")[0]][selAry[key].dataIndex.split("_")[1]];
                        singleDetailStr += "<tr><td class='orderTd'></td><td>单关</td><td><span" + ((selAry[key].pool == "hhad") ? " style='background-color:#CCCCCC;'" : "") + ">" + tmpData.num + "(" + oddsIndex[i] + ")</span>x" + times + "倍</td><td>" + (selAry[key].odds[i] * 2 * Number(times)).toFixed(2) + "</td></tr>";
                    }
                }
            }
        }
        //如果只有单关
        if ($("#optionList input:checked").length == 1) {
            $("#viewDetailTbl").html(singleDetailStr);
            lotFunc.detailTblStyle();
            return;
        }
    }

    var optionStr = lotFunc.getCombinOptStr();

    if (!lotFunc.checkDanCount(optionStr, dAry.length)) {
        $("#detailClose").click();
        return;
    }

    //计算中n场最大最小
    /*
     var mmStr = "";
     for (var i = 2; i <= selAryLen; i++) {
     if (i < Number(optionStr.charAt(0))) continue;
     var mmObj = lotFunc.getMMBonus(optionStr, min_maxAry, i);
     //return { maxBonus: Math.round(maxBonus * 100) / 100, minBonus: Math.round(minBonus * 100) / 100, goalCount: goalCount };
     mmStr += "中" + mmObj.goalCount + "场(" + mmObj.minBonus + "～" + mmObj.maxBonus + ") ";
     }
     $("#mmSpan").html(mmStr);*/

    //计算胆中
    var ary = lotFunc.getMNDetail(dAry, dAry.length + "");
    var combinDAry = ary.combinAry;
    var indexDAry = ary.indexAry;
    var dResultAry = [];
    var poolAry = { "had": false, "hhad": false };
    for (var i = 0; i < indexDAry.length; i++) {
        var oddsIndexStr = indexDAry[i][0] + "";
        var aryIndex = indexDAry[i][1];
        var fOdds = 1;
        var matchIndexStr = "";
        var tmpStr = "";
        for (var j = 0; j < oddsIndexStr.length; j++) {
            matchIndexStr += combinDAry[aryIndex][j];
            var tmpObj = dAry[combinDAry[aryIndex][j]];

            var oIndex;
            if (isNaN(Number(oddsIndexStr.charAt(j)))) {
                oIndex = Number(LetterAry[oddsIndexStr.charAt(j)]) - 1;
            } else {
                oIndex = Number(oddsIndexStr.charAt(j)) - 1;
            }

            poolAry[tmpObj.pool] = true;
            var tmpOddsAry = [];
            for (var m = 0; m < tmpObj.odds.length; m++) {
                if (tmpObj.odds[m] != "") {
                    tmpOddsAry.push({ "value": tmpObj.odds[m], "cn": oddsIndex[m] });
                }
            }

            fOdds *= Number(tmpOddsAry[oIndex].value);
            var tmpData = curData[tmpObj.dataIndex.split("_")[0]][tmpObj.dataIndex.split("_")[1]];
            var bgstr = "";
            if (tmpObj.pool == "hhad") {
                bgstr = " background-color:#CCCCCC;";
            }
            tmpStr += "<span title='" + tmpOddsAry[oIndex].value + "' style='color:red;" + bgstr + "'>" + tmpData.num + "(" + tmpOddsAry[oIndex].cn + ")</span>x";
        }
        dResultAry.push({ "str": tmpStr, "bonus": fOdds, "pool": poolAry });
    }

    //计算拖中
    var combinOpt = "";
    for (var i = 0; i < optionStr.length; i++) {
        var tmpOpt = Number(optionStr.charAt(i)) - dAry.length;
        if (tmpOpt > 0) {
            combinOpt += tmpOpt;
        }
    }
    ary = lotFunc.getMNDetail(tAry, combinOpt);
    var combinAry = ary.combinAry;
    var indexAry = ary.indexAry;
    var resultStr = "";
    if (indexAry.length > 0) {
        for (var i = 0; i < indexAry.length; i++) {
            var oddsIndexStr = indexAry[i][0] + "";
            var aryIndex = indexAry[i][1];
            var fOdds = 1;
            var tmpStr = "";
            var matchIndexStr = "";
            var poolAry = { "had": false, "hhad": false };
            for (var j = 0; j < oddsIndexStr.length; j++) {
                matchIndexStr += combinAry[aryIndex][j];
                var tmpObj = tAry[combinAry[aryIndex][j]];
                var oIndex;
                if (isNaN(Number(oddsIndexStr.charAt(j)))) {
                    oIndex = Number(LetterAry[oddsIndexStr.charAt(j)]) - 1;
                } else {
                    oIndex = Number(oddsIndexStr.charAt(j)) - 1;
                }

                if (curPool.indexOf("had") != -1) poolAry[tmpObj.pool] = true;
                var tmpOddsAry = [];
                for (var m = 0; m < tmpObj.odds.length; m++) {
                    if (tmpObj.odds[m] != "") {
                        tmpOddsAry.push({ "value": tmpObj.odds[m], "cn": oddsIndex[m] });
                    }
                }

                fOdds *= Number(tmpOddsAry[oIndex].value);
                var tmpData = curData[tmpObj.dataIndex.split("_")[0]][tmpObj.dataIndex.split("_")[1]];
                var bgstr = "";
                if (tmpObj.pool == "hhad") {
                    bgstr = " style='background-color:#CCCCCC;padding:1px;border:solid 1px #AAAAAA'";
                }
                tmpStr += "<span" + bgstr + " title='" + tmpOddsAry[oIndex].value + "'>" + tmpData.num + "(" + tmpOddsAry[oIndex].cn + ")</span>x";
            }
            if (dResultAry.length > 0) {
                //添加胆中条目
                for (var n = 0; n < dResultAry.length; n++) {
                    resultStr += "<tr><td class='orderTd'></td><td>" + lotFunc.getPoolTypeStr(poolAry, dResultAry[n]) + (oddsIndexStr.length + dAry.length) + "x1</td><td>" + (tmpStr + dResultAry[n].str + times + "倍") + "</td><td>" + lotFunc.oddsMNLimit(fOdds * dResultAry[n].bonus, oddsIndexStr.length + dAry.length).toFixed(2) + "</td></tr>";
                }
            } else {
                resultStr += "<tr><td class='orderTd'></td><td>" + lotFunc.getPoolTypeStr(poolAry, null) + (oddsIndexStr.length + dAry.length) + "x1</td><td>" + tmpStr + times + "倍" + "</td><td>" + lotFunc.oddsMNLimit(fOdds, oddsIndexStr.length).toFixed(2) + "</td></tr>";
            }
        }

        //
        if (optionStr.indexOf("" + dAry.length) != -1) {
            for (var n = 0; n < dResultAry.length; n++) {
                resultStr += "<tr><td class='orderTd'></td><td>" + lotFunc.getPoolTypeStr(null, dResultAry[n]) + dAry.length + "x1</td><td>" + (dResultAry[n].str + times + "倍") + "</td><td>" + lotFunc.oddsMNLimit(dResultAry[n].bonus, dAry.length).toFixed(2) + "</td></tr>";
            }
        }

    } else { //拖为0
        for (var n = 0; n < dResultAry.length; n++) {
            var tmpStr = "<tr><td class='orderTd'></td><td>" + lotFunc.getPoolTypeStr(null, dResultAry[n]) + dAry.length + "x1</td><td>";
            resultStr += (tmpStr + dResultAry[n].str + times + "倍") + "</td><td>" + lotFunc.oddsMNLimit(dResultAry[n].bonus, dAry.length).toFixed(2) + "</td></tr>";
        }
    }
    $("#viewDetailTbl").html(singleDetailStr + resultStr);
    lotFunc.detailTblStyle();
}
lotFunc.detailTblStyle = function() {

    if (curPool.indexOf("had") != -1) {
        $("#dch").html("注项内容 (背景<span style='background-color:#CCCCCC'>&nbsp;&nbsp;</span>为 让球胜平负)");
    }
    //
    var obj = $("#viewDetailTbl td.orderTd");
    for (var i = 0; i < obj.length; i++) {
        obj.eq(i).html(i + 1);
    }
    //
    $("#viewDetailDiv").show();
    $("#detailList").css("height", "auto");
    //
    if ($("#detailList").height() > 300) {
        $("#detailList").css("overflow", "auto");
        $("#detailList").css("height", "300px");
        $("#dBTd").width(67);
    } else {
        $("#dBTd").width(50);
    }
    //
    $("#viewDetailTbl td:eq(1)").width(100);
    $("#viewDetailTbl td:eq(2)").width(380);
    $("#viewDetailTbl td:eq(3)").width(50);

    //
    setTimeout(function() {
        //
        $("#detailClose").focus();
    }, 500);
}

lotFunc.animate = function(topP, leftP,w,h) {
    if ($("#selDetailDiv").css("display") == "none") {
        //动态效果
        $("#animateFrame").show();
        $("#animateFrame").offset({ top: topP, left: leftP });
        $("#animateFrame").width(w - 2);
        $("#animateFrame").height(h - 2);
        $("#animateFrame").animate({
            left: $("#selDetailBtn").offset().left + 50,
            top: $("#selDetailBtn").offset().top,
            height: '10px',
            width: '10px'
        }, "normal", function() { $("#animateFrame").hide(); });
    }
}
//
lotFunc.setAnn = function(anns) {
    if (anns.data != undefined && anns.data.data != "") {
        var htmStr = "<marquee onmouseover=\"this.stop()\" onmouseout=\"this.start()\" scrolldelay=\"20\" scrollamount=\"3\" direction=\"left\">";
        htmStr += anns.data.data + " ";
        htmStr += "</marquee>";
        $("#noticeSpan").html(htmStr + "&nbsp;<a href='//info.sporttery.cn/iframe/lottery_notice.php  ' target='_blank'>更多>></a>");
        return true;
    } else {
        return false;
    }
}
//
lotFunc.updateTime = function(timeStr) {
    $("#updateTime").html(timeStr);
}
//
lotFunc.copyToClipBoard = function(tableid) {
    var s = "复制到剪贴板";
    if (window.clipboardData) {
        window.clipboardData.setData("Text", s);
        alert("已经复制到剪切板！" + "\n" + s);
    } else if (navigator.userAgent.indexOf("Opera") != -1) {
        window.location = s;
    } else if (window.netscape) {
        try {
            netscape.security.PrivilegeManager.enablePrivilege("UniversalXPConnect");
        } catch (e) {
            alert("被浏览器拒绝！\n请在浏览器地址栏输入'about:config'并回车\n然后将'signed.applets.codebase_principal_support'设置为'true'");
        }
        var clip = Components.classes['@mozilla.org/widget/clipboard;1'].createInstance(Components.interfaces.nsIClipboard);
        if (!clip)
            return;
        var trans = Components.classes['@mozilla.org/widget/transferable;1'].createInstance(Components.interfaces.nsITransferable);
        if (!trans)
            return;
        trans.addDataFlavor('text/unicode');
        var str = new Object();
        var len = new Object();
        var str = Components.classes["@mozilla.org/supports-string;1"].createInstance(Components.interfaces.nsISupportsString);
        var copytext = s;
        str.data = copytext;
        trans.setTransferData("text/unicode", str, copytext.length * 2);
        var clipid = Components.interfaces.nsIClipboard;
        if (!clip)
            return false;
        clip.setData(trans, null, clipid.kGlobalClipboard);
        alert("已经复制到剪切板！" + "\n" + s)
    }
}
//单关不设胆
lotFunc.danCheck = function() {
    if ($("#selDetailTbl .danChk").attr("disabled") != undefined) return;
    ////单关不需设胆
    if (($("#optionList input:eq(0)").prop("checked") == true && $("#optionList input:checked").length == 1) || ($("#optionHeader :checked").attr("index") == 1)) {
        $("#selDetailTbl .danChk").attr("disabled", "disabled");
    } else {
        $("#selDetailTbl .danChk").removeAttr("disabled");
    }
}
//
//欧赔
function getReferData(backData) {
    var oobj = $("#mainTbl td.uOddsTd span");
    oobj.html("--");
    oobj.removeAttr("title");
    var resultObj = backData.data;
    for (var key in resultObj) {
        var tmpObj = $("#list_" + key + " td.uOddsTd span");
        if (curPool == "crs") tmpObj = $("#list_" + key).closest("tr").prev().find("td.uOddsTd span");
        for (var i = 0; i < tmpObj.length; i++) {
            if (i < 3) {
                tmpObj.eq(i).html(resultObj[key]["o" + (i + 1)]);
            }
        }
    }
}
//投注比例
function getReferData1(backData) {
    var resultObj = backData.value;
    $("#mainTbl td.uOddsTd span").html("--");
    for (var key in resultObj) {
        var tmpObj = $("#list" + key + " td.uOddsTd span");
        if (curPool == "crs") tmpObj = $("#list" + key).closest("tr").prev().find("td.uOddsTd span");
        if (resultObj[key]["HAD"] != undefined) {
            tmpObj.eq(0).html(resultObj[key]["HAD"]["hSupportRate"]);
            tmpObj.eq(0).attr("title", resultObj[key]["HAD"]["win"]);
            tmpObj.eq(1).html(resultObj[key]["HAD"]["dSupportRate"]);
            tmpObj.eq(1).attr("title", resultObj[key]["HAD"]["draw"]);
            tmpObj.eq(2).html(resultObj[key]["HAD"]["aSupportRate"]);
            tmpObj.eq(2).attr("title", resultObj[key]["HAD"]["lose"]);
        }
        if (resultObj[key]["HHAD"] != undefined) {
            tmpObj.eq(3).html(resultObj[key]["HHAD"]["hSupportRate"]);
            tmpObj.eq(3).attr("title", resultObj[key]["HHAD"]["win"]);
            tmpObj.eq(4).html(resultObj[key]["HHAD"]["dSupportRate"]);
            tmpObj.eq(4).attr("title", resultObj[key]["HHAD"]["draw"]);
            tmpObj.eq(5).html(resultObj[key]["HHAD"]["aSupportRate"]);
            tmpObj.eq(5).attr("title", resultObj[key]["HHAD"]["lose"]);
        }
    }
}
//判读是否全是单固
lotFunc.isAllSingle = function() {
    var allSingle = true;
    for (var key in selAry) {
        if (selAry[key].single == false) {
            allSingle = false;
            break;
        }
    }
    return allSingle;
}
lotFunc.sortByDefault = function() {
    for (var i = 0; i < curData.length; i++) {
        lotFunc.mySort(curData[i], "num", "asc");
    }
    initData();
    lotFunc.checkToolChk();
    lotFunc.applaySel();
    //
    //
    $("#sortList").offset({ left: 0, top: 0 });
    $("#sortList").hide();
}
//
function scroll(p) {
    var d = document, w = window, o = d.getElementById(p.id), ie6 = /msie 6/i.test(navigator.userAgent);
    if (o) {
        o.style.cssText += ";position:" + (p.f && !ie6 ? 'fixed' : 'absolute') + ";" + (p.r ? 'left' : "right") + ":0;" + (p.t != undefined ? 'top:' + p.t + 'px' : 'bottom:0');
        if (!p.f || ie6) {
            -function() {
                var t = 500, st = d.documentElement.scrollTop || d.body.scrollTop, c;
                c = st - 200 - o.offsetTop + (p.t != undefined ? p.t : (w.innerHeight || d.documentElement.clientHeight) - o.offsetHeight); //如果你是html 4.01请改成d.body，这里不处理以减少代码
                c != 0 && (o.style.top = o.offsetTop + Math.ceil(Math.abs(c) / 10) * (c < 0 ? -1 : 1) + 'px', t = 10);
                setTimeout(arguments.callee, t);
            } ();
        }
    }
}
//
//补长度
function getFixedLength(len, str) {
    var n = str.replace(/[^\x00-\xff]/g, "xx").length;
    if (n < len) {
        for (var i = 0; i < len - n; i++) {
            str += " ";
        }
    }
    return str;
}
//判断ie6
lotFunc.isIE6 = function() {
    var browser = navigator.appName
    var b_version = navigator.appVersion
    var version = b_version.split(";");
    var trim_Version = version;
    if (version.length > 1) {
        trim_Version = version[1].replace(/[ ]/g, "");
    }
    if (browser == "Microsoft Internet Explorer" && trim_Version == "MSIE6.0") {
        return true;
    }
    return false;
}
lotFunc.isIE6();
//测试用
lotFunc.debug = function(info) {
    if (! -[1, ]) {
        alert(info);
    } else {
        console.info(info);
    }
}
//
lotFunc.has_flash = function() {
    var isIE = ! -[1, ];
    if (isIE) {
        try {
            return !!new ActiveXObject('ShockwaveFlash.ShockwaveFlash');
        }
        catch (e) {
        }
    }
    else {
        try {
            return !!navigator.plugins['Shockwave Flash'];
        }
        catch (e) {
        }
    }
    return false;
}
function toArrData(id){
    window.open("//www.sporttery.cn/jc/zqdz/index.html?showType=2&mid="+id);
}
function stopEvent(e){ //阻止冒泡事件
    //取消事件冒泡
    var theEvent = window.event || e;
    if (theEvent && theEvent.stopPropagation) {
        theEvent.stopPropagation();
    } else if (window.event) {
        window.event.cancelBubble = true;
    }
}

var flist = [];
function getTarget(mid,lname,lid,bgColor){
    var hstr = '//www.sporttery.cn/zqlszl/index.html?leagueId='+lid+'&mid='+mid;
    str = "<a  href='"+hstr+"'  target='_blank' style='color: #FFFFFF;display: inline-block; padding:3px; inline-block;width:54px; border-radius:4px; background-color:#" + bgColor + ";'>" + lname + "</a>";
    return str;
}
lotFunc.getJsqConfigData=function (){
    commonV1Fun.ajaxFun(
        lotFunc.setConfigData,
        jsCommonDataV1.webApi+'/gateway/report/getVtoolsConfigV1.qry?configKey=vtools:config:zc_app_loty_betshu',
        undefined,
        'get'
    )
}
lotFunc.setConfigData = function(data){
    if (data.errorCode == 0 && JSON.stringify(data.value) != '{}') {
        if(data.value.zc_app_loty_betshu.length>0){
            if(data.value.zc_app_loty_betshu[0].jczq_offline_max !='' && data.value.zc_app_loty_betshu[0].jczq_offline_max !=undefined){
                maxTimes = Number(data.value.zc_app_loty_betshu[0].jczq_offline_max);//最大倍数
            }
            if(JSON.stringify(data.value.zc_app_loty_betshu[0].amountInfos) != '{}' 
            && JSON.stringify(data.value.zc_app_loty_betshu[0].amountInfos.jczq_offline != '{}') 
            && data.value.zc_app_loty_betshu[0].amountInfos.jczq_offline.amount_limit !=''){
                Limit = Number(data.value.zc_app_loty_betshu[0].amountInfos.jczq_offline.amount_limit);
            } 
            $("#limitText").text("[最高" + maxTimes + "倍]"); 
        }  
    }
}
// 对象返回最大的计税期号
lotFunc.getMaxTaxNo = function (obj){
    let maxValue = null;
    let maxKey = null;
    var selectLen = $("#optionList input:checked").length;
    for (let key in obj) {
        if($("#optionList input:eq(0)").prop("checked") == true && selectLen ==1 ){
            if(obj[key].single){
                if (obj[key].hasOwnProperty('taxDateNo') && typeof (parseInt(obj[key].taxDateNo)) =='number') {
                    if (maxValue === null || obj[key].taxDateNo > maxValue) {
                        maxValue = obj[key].taxDateNo;
                        maxKey = key;
                    }
                }
            }
        }else{
            if (obj[key].hasOwnProperty('taxDateNo') && typeof (parseInt(obj[key].taxDateNo)) =='number') {
                if (maxValue === null || obj[key].taxDateNo > maxValue) {
                    maxValue = obj[key].taxDateNo;
                    maxKey = key;
                }
            }
        }

    }
    // for (let key in obj) {
    //     if (obj[key].hasOwnProperty('taxDateNo') && typeof (parseInt(obj[key].taxDateNo)) == 'number') {
    //         if (maxValue === null || obj[key].taxDateNo > maxValue) {
    //             maxValue = obj[key].taxDateNo;
    //             maxKey = key;
    //         }
    //     }
    // }
    return { maxValue, maxKey }

};
// 选择场景
lotFunc.showTaxDateNo = function (){
    if ($("#optionList input:checked").length == 0) {
        lotFunc.showTaxDateNoHtml('')
    }
    var selectLen = $("#optionList input:checked").length;

    if(selectLen > 0){
        var taxDateNo = lotFunc.getMaxTaxNo(selAry);
        if(taxDateNo.hasOwnProperty('maxValue') && taxDateNo.maxValue ==''){
            return false
        }
        if($("#optionList input:eq(0)").prop("checked") == true && selectLen ==1 ){
            lotFunc.showTaxDateNoHtml(taxDateNo)
        }else {
            if($("#optionList input:eq(0)").prop("checked") == true){
                lotFunc.showTaxDateNoHtml('no')
            }else{
                if ($("#optionList input:checked").length >= 1) {
                    lotFunc.showTaxDateNoHtml(taxDateNo)
                }
            }
        }
    }
}
// 显示期号及提示
lotFunc.showTaxDateNoHtml = function (val) {
    var txt = '该方案涉及多张票，期号详见票面显示。'
    $('#taxno').html('')
    $('#taxdesc').html('')
    if(val ==='no'){
        $('#taxdesc').html(txt)
        $('#viewDetailDiv').css('bottom', $("#sel_pan").css('height'))
        $('#selDetailDiv').css('bottom', $("#sel_pan").css('height'))
    }else{
        if(val.hasOwnProperty('maxValue') && isStringNumber(val.maxValue)){
            $('#taxno').html('第'+val.maxValue+'期（仅供参考）<br/>')
        }else{
            $('#taxno').html('')
        }
        $('#viewDetailDiv').css('bottom', $("#sel_pan").css('height'))
        $('#selDetailDiv').css('bottom', $("#sel_pan").css('height'))
    }

}
// 判断是否为数字
function isStringNumber(val) {
    return !isNaN(parseFloat(val)) && isFinite(val);
}
$(document).ready(function () {
    $(window).resize(function () {
        lotFunc.scrollFunc();
    });
})
