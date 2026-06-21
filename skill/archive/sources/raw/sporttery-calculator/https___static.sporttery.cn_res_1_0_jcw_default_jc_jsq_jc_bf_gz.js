function init() {
    oddsName = { "0": "s0", "1": "s1", "2": "s2", "3": "s3", "4": "s4", "5": "s5", "6": "s6", "7+": "s7" };
    oddsIndex = ["1:0", "2:0", "2:1", "3:0", "3:1", "3:2", "4:0", "4:1", "4:2", "5:0", "5:1", "5:2", "胜其它", "0:0", "1:1", "2:2", "3:3", "平其它", "0:1", "0:2", "1:2", "0:3", "1:3", "2:3", "0:4", "1:4", "2:4", "0:5", "1:5", "2:5", "负其它"];
    curPool = "crs";
}
$(document).ready(function() {

    //
    lotFunc.ready();
    //

    $("#mainTbl").on("click","span.oddsItem", function(e) {
        if ($(this).hasClass("oddsDis")) return;
        if ($(this).text() == "" || $(this).text() == "--") return;
        if ($(this).hasClass("oddsEffect")) return;
        var index = $(this).closest("table").find("span").index($(this));
        var tmpAry = $(this).closest("table").attr("id").split("_");
        var idStr = tmpAry[1];
        if ($(this).hasClass("oddsClk")) { //不选
            $(this).removeClass("oddsClk");
            if (selAry[idStr] == undefined) {

            } else {
                var oddsAry = selAry[idStr].odds;
                oddsAry[index] = "";
                if (Number(oddsAry.join("")) == 0) {
                    delete selAry[idStr];
                    selAryLen--;
                }
            }
        } else {
            if (selAry[idStr] == undefined) {
                //
                var allSingle = lotFunc.isAllSingle();

                if ($(this).closest(".crsOddsTr").prev().find(".vsTd").css("background-image") == "none") {
                    allSingle = false;
                }
                //超过n场
                if (selAryLen >= 4) {
                    if (!allSingle) {
                        alert("超过4场只能选择单关进行计算");
                        return;
                    }
                }
                //
                selAry[idStr] = new Object();
                selAry[idStr].odds = ["", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", ""];
                selAry[idStr].dataIndex = $(this).closest("table").attr("dataIndex");
                selAry[idStr].pool = "crs";
                selAryLen++;
            }
            selAry[idStr].odds[index] = $(this).html().toLocaleLowerCase().split("</div>")[1];
            var vsStr = $(this).closest(".crsOddsTr").prev().find(".vsTd").css("background-image");
            if (vsStr != "none") {
                selAry[idStr].single = true;
            } else {
                selAry[idStr].single = false;
            }
            selAry[idStr].matchNumDate = $(this).closest("table").attr("matchNumDate");
            selAry[idStr].taxDateNo = $(this).closest("table").attr("taxDateNo");

            $(this).addClass("oddsClk");
            //
            lotFunc.animate($(this).offset().top, $(this).offset().left, $(this).width(), $(this).height());
        }
        lotFunc.calculate();
        //
        //
        if ($("#optionTip").html() != "") {
            $("#optionTip").width(100);
            $("#optionTip").animate({ width: "280px" }, { easing: 'easeOutBounce', duration: 600, complete: null });
        }
    });
    //
    $("#mainTbl").on("click", " a.crsClearAll",function() {
        $(this).closest("table").find("span.oddsClk").click();
    });
    //
    $("#mainTbl").on("click"," span.folderTd", function() {
        var str = $(this).attr("title");
        var trObj = $(this).closest("tr").next();
        if (str == "展开") {
            //加载数据
            var tblObj = trObj.find("table");
            if (tblObj.find("tr").length == 0) {
                var dataIndex = tblObj.attr("dataindex").split("_");
                tblObj.html(initCrsOdds(dataIndex[0], dataIndex[1]));
            }
            //
            trObj.show();
            $(this).attr("title", "收起");
            $(this).text("-");
        } else {
            trObj.hide();
            $(this).attr("title", "展开");
            $(this).text("+");
        }
        lotFunc.scrollFunc();
    });
    //全部展开/关闭
    $("#openCrsAll").click(function() {
        var listTrObj = $("#mainTbl tr.listTr:visible");
        if ($(this).text() == "+") {
            listTrObj.next().show();
            listTrObj.find("span.folderTd").attr("title", "收起");
            listTrObj.find("span.folderTd").text("-");
            $(this).text("-");
            $(this).attr("title", "全部收起");
            //
            for (var i = 0; i < listTrObj.length; i++) {
                var tblObj = listTrObj.eq(i).next().find("table");
                if (tblObj.find("tr").length == 0) {
                    var dataIndex = tblObj.attr("dataindex").split("_");
                    tblObj.html(initCrsOdds(dataIndex[0], dataIndex[1]));
                }
            }
        } else {
            listTrObj.next().hide();
            listTrObj.find("span.folderTd").attr("title", "展开");
            listTrObj.find("span.folderTd").text("+");
            $(this).text("+");
            $(this).attr("title", "全部展开");
        }

    });
});
function initCrsOdds(i, j) {
    var tmpObj = curData[i][j];
    var disCls = "";
    if (tmpObj.crs != undefined && tmpObj.crs.cbt == "2") disCls = " oddsDis";
    return "<tr><td>胜</td><td style='width:1080px;text-align:left;'><span class='oddsItem oddsItemKeep" + disCls + "'><div>1:0</div>" + tmpObj.crs["0100"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>2:0</div>" + tmpObj.crs["0200"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>2:1</div>" + tmpObj.crs["0201"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>3:0</div>" + tmpObj.crs["0300"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>3:1</div>" + tmpObj.crs["0301"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>3:2</div>" + tmpObj.crs["0302"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>4:0</div>" + tmpObj.crs["0400"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>4:1</div>" + tmpObj.crs["0401"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>4:2</div>" + tmpObj.crs["0402"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>5:0</div>" + tmpObj.crs["0500"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>5:1</div>" + tmpObj.crs["0501"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>5:2</div>" + tmpObj.crs["0502"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>胜其它</div>" + tmpObj.crs["-1-h"] + "</span></td></tr>" +
        "<tr><td>平</td><td style='text-align:left;'><a href='javascript:void(0);' class='crsClearAll'>[全部清除]</a><span class='oddsItem oddsItemKeep" + disCls + "'><div>0:0</div>" + tmpObj.crs["0000"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>1:1</div>" + tmpObj.crs["0101"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>2:2</div>" + tmpObj.crs["0202"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>3:3</div>" + tmpObj.crs["0303"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>平其它</div>" + tmpObj.crs["-1-d"] + "</span></td></tr>" +
        "<tr><td>负</td><td style='text-align:left;'><span class='oddsItem oddsItemKeep" + disCls + "'><div>0:1</div>" + tmpObj.crs["0001"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>0:2</div>" + tmpObj.crs["0002"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>1:2</div>" + tmpObj.crs["0102"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>0:3</div>" + tmpObj.crs["0003"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>1:3</div>" + tmpObj.crs["0103"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>2:3</div>" + tmpObj.crs["0203"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>0:4</div>" + tmpObj.crs["0004"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>1:4</div>" + tmpObj.crs["0104"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>2:4</div>" + tmpObj.crs["0204"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>0:5</div>" + tmpObj.crs["0005"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>1:5</div>" + tmpObj.crs["0105"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>2:5</div>" + tmpObj.crs["0205"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>负其它</div>" + tmpObj.crs["-1-a"] + "</span></td></tr>";
}
function getData(backData) {
    if (backData == undefined || backData == "" || backData.data == undefined || backData.data == "" || (backData.data.length != undefined && backData.data.length == 0)) {
        //alert("没有比赛数据！");
        $("#mainTbl").html(noMatchShow);
        return;
    }
    lotFunc.updateTime(backData.status.last_updated);
    var newDataFlag = Boolean(curData.length);
    if (!newDataFlag) {
        var bDate = "";
        for (var key in backData.data) {
            var tmpObj = backData.data[key];
            if (tmpObj.b_date != bDate) {
                curData.push([]);
                bDate = tmpObj.b_date;
            }
            curData[curData.length - 1].push(tmpObj);
        }
    }
    initData();
}
function initData() {
    if (curData.length == 0) {
        return;
    }
    filteAry = { date: [], league: [] };
    var htmlStr = "";
    for (var i = 0; i < curData.length; i++) {
        var tmpDate = curData[i][0].b_date;
        var weekObj = lotFunc.getWeek(tmpDate);
        filteAry.date.push(weekObj.cn);
        var matchNumDate = curData[i][0].match_num_date
        htmlStr += "<tr><td class='bDateTd' colspan='9' bIndex='" + filteAry.date.length + "'>周" + weekObj.cn + " " + tmpDate + " 共<label></label>场比赛 "
        if(matchNumDate !='' && matchNumDate != undefined){
            htmlStr += '<span class="match-date-num">(比赛编号日期：'+ matchNumDate +")</span>"
        }
        htmlStr +="<a href='javascript:void(0);' class='bDateHide'>[隐藏]</a>"
        htmlStr += "</td></tr>";
        for (var j = 0; j < curData[i].length; j++) {
            var tmpObj = curData[i][j];

            var isFind = -1;
            for (var m = 0; m < filteAry.league.length; m++) {
                if (filteAry.league[m] == tmpObj.l_cn) {
                    isFind = m + 1;
                    break;
                }
            }
            if (isFind == -1) {
                filteAry.league.push(tmpObj.l_cn);
                isFind = filteAry.league.length;
            }
            // var weatherInfo = "未知";
            // if (tmpObj.weather_pic != undefined) {
            //     weatherInfo = "<img style='vertical-align:middle' title='" + tmpObj.weather + "' src='" + tmpObj.weather_pic + "' />";
            // }
            var matchInfo = "&nbsp;";
            if (tmpObj.match_info) {
                matchInfo += "<span style='cursor:pointer; background:none !important' title='" + tmpObj.match_info + "'><img src='//static.sporttery.cn/res_1_0/jcw/upload/202209/fb_alert.png'></span>";
                // for (var m = 0; m < tmpObj.match_info.length; m++) {
                //     matchInfo += "<span style='cursor:pointer;' title='" + tmpObj.match_info[m].prompt + "'>中立</span>";
                // }
            }
            var disCls = "";
            if (tmpObj.crs != undefined && tmpObj.crs.cbt == "2") disCls = " oddsDis";
            var openStr = ((i == 0 && j == 0) ? ("<span title='收起' class='folderTd'>-</span>") : ("<span title='展开' class='folderTd'>+</span>"));
            var bgStr = "";
            var tmpNum = Math.round(Math.random() * 10); //测试数据 || tmpNum % 7 == 0
            if ((tmpObj.crs.single == "1" && tmpObj.crs.o_type == "F")) {
                bgStr = "background-image:url("+jsCommonDataV1.resDomain+"/res_1_0/jcw/images/jc_calculator/single.gif);background-repeat:no-repeat;background-position:left top;";
            }
            htmlStr += "<tr class='listTr' lIndex='" + isFind + "' bIndex='" + filteAry.date.length + "'><td" + ((i == 0) ? " style='width:68px;'" : "") + ">" + tmpObj.num + "</td><td title='" + tmpObj.l_cn + "' class='lname' style='color:#FFFFFF;width:89px' >"
                +getTarget(tmpObj.id,tmpObj.l_cn_abbr,tmpObj.l_id, tmpObj.l_background_color)+"</td><td style='color:width:120px' >" + tmpObj.date.substr(5)
                + " " + tmpObj.time.substr(0, 5) + "</td>" +
                "<td  title='点击VS进入对阵数据页,点击球队名称进入球队介绍页' onclick='toArrData("+tmpObj.id+")' class='vsTd' style='"
                + ((i == 0) ? "width:487px;" : "") + bgStr + "'>"
                + "<span class='match-left'>"

                +'<img src="https://static.sporttery.cn/res_1_0/jcw/images/gz/qdmgz.png" class="subscribe-icon img_' + tmpObj.h_id + '" onclick="stopEvent(event);bfGz.subscribeTeam(\'' + tmpObj.h_id + '\', \'' + tmpObj.h_cn_abbr + '\', 0, \'' + tmpObj.l_id + '\')">'
            if(tmpObj.h_order !=''){
                htmlStr +=  "<label style='padding-left: 4px'>" + tmpObj.h_order + "</label>"
            }
            htmlStr += "<a onclick='stopEvent(event)'   href='//www.sporttery.cn/zqlszl/qdzl/index.html?gmtid="
                +tmpObj.h_id+"' target='_blank' >"
                + "<span class=\"vs-left-padding\">" +tmpObj.h_cn_abbr + "</span></a> "
                +"</span>"
                +"<a  onclick='stopEvent(event)' class='vsA' target='_blank' href='//www.sporttery.cn/jc/zqdz/index.html?showType=2&mid="
                +tmpObj.id+"'>VS</a> "
                + "<span class='match-right'>"
                +"<a  onclick='stopEvent(event)' href='//www.sporttery.cn/zqlszl/qdzl/index.html?gmtid="+tmpObj.a_id+"'  target='_blank' >"
                + "<span class=\"vs-right-padding\">" +tmpObj.a_cn_abbr + "</span></a>"

            if(tmpObj.a_order !=''){
                htmlStr +=  "<label style='padding-right: 4px'>" + tmpObj.a_order + "</label>"
            }
            htmlStr +='<img src="https://static.sporttery.cn/res_1_0/jcw/images/gz/qdmgz.png" class="subscribe-icon img_' + tmpObj.a_id + '" onclick="stopEvent(event);bfGz.subscribeTeam(\'' + tmpObj.a_id + '\', \'' + tmpObj.a_cn_abbr + '\', 0, \'' + tmpObj.l_id + '\')">'
            htmlStr += "</span></td>" +
                "<td class='linkTd'" + ((i == 0) ? " style='width:88px;'" : "")
                + "><a href='//www.sporttery.cn/jc/zqtjhc/?m=" + tmpObj.id + "' target='_blank'>同奖</a></td><td class='uOddsTd' style='width:145px'><div class='ttgU'><span class='rLine' style='width:48px' >--</span><span class='rLine' style='width:48px'>--</span><span style='width:48px'>--</span></div></td><td class='matchInfoTd'" + ((i == 0) ? " style='width:64px;'" : "") + ">" + matchInfo + "</td><td style='width:72px'><img src=\"https://static.sporttery.cn/res_1_0/jcw/images/gz/scmgz.png\" id='img_" + tmpObj.id + "' class='subscribe-icon-sc img_" + tmpObj.id + "' style='cursor: pointer;' onclick=\"stopEvent(event);bfGz.subscribeMatch('" + tmpObj.l_id + "', '" + tmpObj.id + "', '" + tmpObj.match_num + "')\"></td><td style='width:55px' " + ((i == 0) ? " style='width:35px;'" : "") + ">" + openStr + "</td></tr>";                    if (i == 0 && j == 0) {
                htmlStr += "<tr class='crsOddsTr'><td colspan='9' style='padding:8px;'>" +
                    "<table id='list_" + tmpObj.id + "' cellpadding='0' cellspacing='0' width='100%' class='crsOdds' dataIndex='" + i + "_" + j + "'  matchNumDate='"+tmpObj.match_num_date+"' taxDateNo='"+tmpObj.tax_date_no+"'><tr><td>胜</td><td style='width:1080px;text-align:left;'><span class='oddsItem oddsItemKeep" + disCls + "'><div>1:0</div>" + tmpObj.crs["0100"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>2:0</div>" + tmpObj.crs["0200"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>2:1</div>" + tmpObj.crs["0201"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>3:0</div>" + tmpObj.crs["0300"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>3:1</div>" + tmpObj.crs["0301"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>3:2</div>" + tmpObj.crs["0302"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>4:0</div>" + tmpObj.crs["0400"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>4:1</div>" + tmpObj.crs["0401"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>4:2</div>" + tmpObj.crs["0402"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>5:0</div>" + tmpObj.crs["0500"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>5:1</div>" + tmpObj.crs["0501"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>5:2</div>" + tmpObj.crs["0502"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>胜其它</div>" + tmpObj.crs["-1-h"] + "</span></td></tr>" +
                    "<tr><td>平</td><td style='text-align:left;'><a href='javascript:void(0);' class='crsClearAll'>[全部清除]</a><span class='oddsItem oddsItemKeep" + disCls + "'><div>0:0</div>" + tmpObj.crs["0000"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>1:1</div>" + tmpObj.crs["0101"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>2:2</div>" + tmpObj.crs["0202"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>3:3</div>" + tmpObj.crs["0303"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>平其它</div>" + tmpObj.crs["-1-d"] + "</span></td></tr>" +
                    "<tr><td style='border-bottom:none;'>负</td><td style='text-align:left;border-bottom:none;'><span class='oddsItem oddsItemKeep" + disCls + "'><div>0:1</div>" + tmpObj.crs["0001"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>0:2</div>" + tmpObj.crs["0002"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>1:2</div>" + tmpObj.crs["0102"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>0:3</div>" + tmpObj.crs["0003"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>1:3</div>" + tmpObj.crs["0103"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>2:3</div>" + tmpObj.crs["0203"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>0:4</div>" + tmpObj.crs["0004"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>1:4</div>" + tmpObj.crs["0104"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>2:4</div>" + tmpObj.crs["0204"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>0:5</div>" + tmpObj.crs["0005"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>1:5</div>" + tmpObj.crs["0105"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>2:5</div>" + tmpObj.crs["0205"] + "</span><span class='oddsItem oddsItemKeep" + disCls + "'><div>负其它</div>" + tmpObj.crs["-1-a"] + "</span></td></tr></table></td></tr>";
            } else {
                htmlStr += "<tr class='crsOddsTr'><td colspan='9' style='padding:8px;'>" +
                    "<table id='list_" + tmpObj.id + "' cellpadding='0' cellspacing='0' width='100%' class='crsOdds' dataIndex='" + i + "_" + j + "'  matchNumDate='"+tmpObj.match_num_date+"' taxDateNo='"+tmpObj.tax_date_no+"'></table></td></tr>";
            }
        }
    }
    $("#mainTbl").html(htmlStr);
    //
    $("#mainTbl span.oddsItem").each(function() {
        if ($(this).text() == "") $(this).text("--");
    });
    //
    lotFunc.initOK();
    //
    var hTdObj = $("#headerTr td");
    var mTdObj = $("#mainTbl tr:eq(1) td");
    for (var i = 0; i < hTdObj.length; i++) {
        hTdObj.eq(i).width(mTdObj.eq(i).width());
    }
    //
    if (!loadedOk) {
        if (listTop < 0) listTop = $("#headerTr").offset().top;
        $(window).scroll(lotFunc.scrollFunc);
        setTimeout(lotFunc.scrollFunc, 500);
        loadedOk = true;
    }
    //比分首行展开
    $("#mainTbl tr.crsOddsTr:gt(0)").hide();
    //
    lotFunc.getReferDataI(0);
    bfGz.showHtml = true
}
function setAnn(anns) {
    lotFunc.setAnn(anns);
}
function updateSelDetail() {
    if ($("#selDetailDiv").css("display") == "none") return;
    var str = "<tr style='font-weight:bold;'><td><a href='javascript:void(0)' id='clearSel'>清空</a></td><td>编号</td><td>对阵</td><td>投注选项</td><td>胆<label id='selDetailClose'>×</label></td></tr>";
    var poolStr = "";
    if (selAryLen > 0) {
        for (var key in selAry) {
            var selObj = selAry[key];
            var indexAry = selObj.dataIndex.split("_");
            var tmpAry = selObj.odds;
            var dataObj = curData[indexAry[0]][indexAry[1]];
            var selItemStr = "";
            for (var i = 0; i < tmpAry.length; i++) {
                if (tmpAry[i] != "") {
                    selItemStr += "<span title='" + tmpAry[i] + "' index='" + i + "'>" + oddsIndex[i] + "</span>";
                }
            }
            str += "<tr id='sel_" + key + "'><td class='delSelLine'>×</td><td>" + dataObj.num + "</td><td>" + dataObj.h_cn_abbr + " VS " + dataObj.a_cn_abbr
                + "</td><td class='selItemTd'>" + selItemStr + "</td><td><input type='checkbox'" + ((selObj.isDan) ? " checked" : "")
                + " class='danChk'" + (($("#optionHeader input").attr("disabled") != undefined) ? " disabled" : "") + " /></td></tr>";
        }
    }
    $("#selDetailTbl").html(str);
    lotFunc.autoScroll();
    lotFunc.danCheck();
}

var bfGz = {
    showHtml: false,
    canLoginTag: false,
    canOrderTag: false,
    teamList: [],
    matchList: [],
    refreshDebounceTimer: null,
    subscribeTeam: async function(teamId, teamName, leagueId) {
        if(!bfGz.canLoginTag){
            goLoginFunPage();
            showLoginModal();
            return
        }
        if ( bfGz.refreshDebounceTimer) {
            return;
        }
        bfGz.refreshDebounceTimer = setTimeout(function() {
            bfGz.refreshDebounceTimer = null;
        }, 1500);
        var subscribedImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/qdgz.png';
        var unsubscribedImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/qdmgz.png';
        var imgSelector = '.subscribe-icon.img_' + teamId;

        try {
            var isSubscribe = $(imgSelector).attr('src') !== subscribedImgUrl;
            let objTrack = {
                teamId:teamId,
                leagueId: leagueId,
                status:isSubscribe?1:0
            }
            bfGz.setTrackEvent1('click_jingcaiFollowTeam_button',{'ext1':JSON.stringify(objTrack)})

            if (isSubscribe) {
                $(imgSelector).attr('src', subscribedImgUrl);
                const result = await teamSubscribeSubmitFun(bfGz.scanChannel, '0', teamId, teamName, 0);
                console.log('subscribeTeam 结果:', result);
                if(result.errCode == 0) {
                    bfGz.showToast('订阅关注成功')
                    getSubscribeStatus(result.data)
                } else if(result.errCode == '-20132602'){
                    $(imgSelector).attr('src', unsubscribedImgUrl);
                    bfGz.initSubscribeStatus(bfGz.teamList,2);
                    bfGz.initSubscribeMatchStatus(bfGz.matchList,2);
                    goLoginFunPage();
                    LoginModal.show();
                } else {
                    $(imgSelector).attr('src', unsubscribedImgUrl);
                    bfGz.showToast(result.alert.actionData);
                }
            } else {
                $(imgSelector).attr('src', unsubscribedImgUrl);
                const result = await teamSubscribeCancelFun('0', teamId, 0);
                console.log('subscribeTeamCancel 结果:', result);
                if(result.errCode == 0) {
                    bfGz.showToast('取消订阅关注成功');
                } else if(result.errCode == '-20132602'){
                    $(imgSelector).attr('src', subscribedImgUrl);
                    bfGz.initSubscribeStatus(bfGz.teamList,2);
                    bfGz.initSubscribeMatchStatus(bfGz.matchList,2);
                    goLoginFunPage();
                    LoginModal.show();
                } else {
                    $(imgSelector).attr('src', subscribedImgUrl);
                    bfGz.showToast(result.alert.actionData);
                }
            }
        } catch (error) {
            console.error('subscribeTeam 错误:', error);
        }
    },
    refreshDebounceTimer2:false,
    subscribeMatch: async function(leagueId, matchId, matchNum) {
        if(!bfGz.canLoginTag){
            goLoginFunPage();
            showLoginModal();
            return
        }
        if ( bfGz.refreshDebounceTimer2) {
            return;
        }
        bfGz.refreshDebounceTimer2 = setTimeout(function() {
            bfGz.refreshDebounceTimer2 = null;
        }, 1500);
        var subscribedImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/scgz.png';
        var unsubscribedImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/scmgz.png';
        var imgSelector = '.subscribe-icon-sc.img_' + matchId;

        try {
            var isSubscribe = $(imgSelector).attr('src') !== subscribedImgUrl;
            let objTrack ={
                leagueId:leagueId,
                matchId:matchId,
                matchNum:matchNum,
                status:isSubscribe?1:0
            }
            bfGz.setTrackEvent1('click_jingcaiFollowEvent_button',{'ext1':JSON.stringify(objTrack)})

            if (isSubscribe) {
                $(imgSelector).attr('src', subscribedImgUrl);
                const result = await matchSubscribeSubmitFun(bfGz.scanChannel, '0', JSON.stringify([{leagueId:leagueId, matchId:+matchId, matchNum:+matchNum}]));
                console.log('subscribeMatch 结果:', result);
                if(result.errCode == 0) {
                    bfGz.showToast('订阅关注成功')
                    getSubscribeStatus(result.data)
                } else if(result.errCode == '-20132602'){
                    $(imgSelector).attr('src', unsubscribedImgUrl);
                    bfGz.initSubscribeStatus(bfGz.teamList,2);
                    bfGz.initSubscribeMatchStatus(bfGz.matchList,2);
                    goLoginFunPage();
                    LoginModal.show();
                } else {
                    $(imgSelector).attr('src', unsubscribedImgUrl);
                    bfGz.showToast(result.alert.actionData);
                }
            } else {
                $(imgSelector).attr('src', unsubscribedImgUrl);
                const result = await matchSubscribeCancelFun(bfGz.scanChannel, '0', JSON.stringify([{leagueId:leagueId, matchId:+matchId, matchNum:matchNum}]));
                console.log('subscribeMatchCancel 结果:', result);
                if(result.errCode == 0) {
                    bfGz.showToast('取消订阅关注成功');
                } else if(result.errCode == '-20132602'){
                    $(imgSelector).attr('src', subscribedImgUrl);
                    bfGz.initSubscribeStatus(bfGz.teamList,2);
                    bfGz.initSubscribeMatchStatus(bfGz.matchList,2);
                    goLoginFunPage();
                    LoginModal.show();
                } else {
                    $(imgSelector).attr('src', subscribedImgUrl);
                    bfGz.showToast(result.alert.actionData);
                }
            }
        } catch (error) {
            console.error('subscribeMatch 错误:', error);
        }
    },

    initSubscribeStatus: function(subscribedTeamIds,t) {
        if(t == 1) {
            var subscribedTeamImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/qdgz.png';
            if (subscribedTeamIds && subscribedTeamIds.length > 0) {
                for (var i = 0; i < subscribedTeamIds.length; i++) {
                    var teamId = subscribedTeamIds[i];
                    $('.subscribe-icon.img_' + teamId).attr('src', subscribedTeamImgUrl);
                    $('.subscribe-icon.img_' + teamId).show()
                }
            }
            bfGz.canOrderTag = true
        } else {
            var subscribedTeamImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/qdmgz.png';
            if (subscribedTeamIds && subscribedTeamIds.length > 0) {
                for (var i = 0; i < subscribedTeamIds.length; i++) {
                    var teamId = subscribedTeamIds[i];
                    $('.subscribe-icon.img_' + teamId).attr('src', subscribedTeamImgUrl);
                }
            }
        }

    },
    initSubscribeMatchStatus: function(subscribedMatchIds,t) {
        if(t ==1){
            // 选中
            var subscribedMatchImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/scgz.png';
            $('.subscribe-icon-sc.img_').not('[src*="scgz.png"]').hide();
            if (subscribedMatchIds && subscribedMatchIds.length > 0) {
                for (var i = 0; i < subscribedMatchIds.length; i++) {
                    var matchId = subscribedMatchIds[i];
                    $('.subscribe-icon-sc.img_' + matchId).attr('src', subscribedMatchImgUrl).show();
                }
            }
        } else {
            var subscribedMatchImgUrl = 'https://static.sporttery.cn/res_1_0/jcw/images/gz/scmgz.png';
            if (subscribedMatchIds && subscribedMatchIds.length > 0) {
                for (var i = 0; i < subscribedMatchIds.length; i++) {
                    var matchId = subscribedMatchIds[i];
                    $('.subscribe-icon-sc.img_' + matchId).attr('src', subscribedMatchImgUrl);
                }
            }
        }


    },

    loadSubscribedTeams: async function() {
        if(!bfGz.canLoginTag) {
            return
        }
        try {
            const result = await getSubscribeMatchListFun(
                bfGz.scanChannel,
                '0',
                'current'
            );
            bfGz.matchList = [];
            bfGz.teamList = [];
            console.log('getSubscribeMatchListFun 结果:', result);
            if(result.errCode == 0) {
                if(result.data.matchList && result.data.matchList.length > 0){
                    result.data.matchList.forEach((element, index) => {
                        bfGz.matchList.push(element.matchId)
                    })
                }
                if(result.data.teamList && result.data.teamList.length > 0){
                    result.data.teamList.forEach((element, index) => {
                        bfGz.teamList.push(element.teamId)
                    })
                }
                bfGz.initSubscribeStatus(bfGz.teamList,1);
                bfGz.initSubscribeMatchStatus(bfGz.matchList, 1);
            }
        } catch (error) {
            console.error('getSubscribeMatchListFun 错误:', error);
        }
    },
    showToast: function(message, duration) {
        duration = duration || 1500;

        var toastId = 'bf_gz_toast_' + Date.now();
        var toastHtml = '<div id="' + toastId + '" style="position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%); background: rgba(0, 0, 0, 0.75); color: #fff; padding: 16px 24px; border-radius: 8px; font-size: 14px; z-index: 9999; text-align: center; min-width: 160px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);">' + message + '</div>';

        $('body').append(toastHtml);

        setTimeout(function() {
            $('#' + toastId).fadeOut(300, function() {
                $(this).remove();
            });
        }, duration);
    },
    setTrackEvent1:function(eventName, data={}){
        try{
            dc.trackEvent(eventName, data)
        }catch(error){
            console.log(error.message)
        }
    }
};

$(document).ready(function() {
    bfGz.loadSubscribedTeams();
});

function callListdata() {
    bfGz.scanChannel = channelNameID[window.localStorage.getItem('getScanChannel') || '200000'] || 1;
    bfGz.scanTokenTag = window.sessionStorage.getItem('getScanToken') || '';
    bfGz.canLoginTag = true;
    if(bfGz.showHtml){
        bfGz.loadSubscribedTeams();
    } else {
        const checkBfGz = setInterval(() => {
            if (bfGz.showHtml) {
                clearInterval(checkBfGz);
                bfGz.loadSubscribedTeams();
            }
        }, 100);
    }

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

