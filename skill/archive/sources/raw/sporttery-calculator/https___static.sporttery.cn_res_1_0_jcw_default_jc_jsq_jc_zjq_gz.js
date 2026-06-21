
function init() {
    oddsName = { "0": "s0", "1": "s1", "2": "s2", "3": "s3", "4": "s4", "5": "s5", "6": "s6", "7+": "s7" };
    oddsIndex = ["0", "1", "2","3","4","5","6","7+"];
    curPool = "ttg";
};
$(document).ready(function() {
    //
    lotFunc.ready();
    //
    $("#mainTbl").on("click", "span.oddsItem",function(e) {
        if ($(this).hasClass("oddsDis")) return;
        if ($(this).text() == "" || $(this).text() == "--") return;
        if ($(this).hasClass("oddsEffect")) return;
        var index = $(this).closest("td").find("span").index($(this));
        var tmpAry = $(this).closest("tr").attr("id").split("_");
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
                var allSingle = lotFunc.isAllSingle();
                if ($(this).closest("tr").find(".vsTd").css("background-image") == "none") {
                    allSingle = false;
                }
                //超过n场
                if (selAryLen >= 6) {
                    if (!allSingle) {
                        alert("超过6场只能选择单关进行计算");
                        return;
                    }
                }
                //
                selAry[idStr] = new Object();
                selAry[idStr].odds = ["", "", "", "", "", "", "", ""];
                selAry[idStr].dataIndex = $(this).closest("tr").attr("dataIndex");
                selAry[idStr].pool = "ttg";
                if ($(this).closest("tr").find(".vsTd").css("background-image") != "none") {
                    selAry[idStr].single = true;
                } else {
                    selAry[idStr].single = false;
                }
                selAryLen++;
            }
            selAry[idStr].matchNumDate = $(this).closest("tr").attr("matchNumDate");
            selAry[idStr].taxDateNo = $(this).closest("tr").attr("taxDateNo");
            selAry[idStr].odds[index] = $(this).text();
            $(this).addClass("oddsClk");
            lotFunc.animate($(this).offset().top, $(this).offset().left, $(this).width(), $(this).height());
        }
        lotFunc.calculate();
        //
        if ($("#optionTip").html() != "") {
            $("#optionTip").width(100);
            $("#optionTip").animate({ width: "280px" }, { easing: 'easeOutBounce', duration: 600, complete: null });
        }
    });
    //
    $("#mainTbl").on("mouseenter","span.oddsItem", function() {
        var index = $(this).closest("td").find(".oddsItem").index($(this));
        $("#headerTr span.oddsHeader:eq(" + index + ")").addClass("headerOver");
    });
    //
    $("#mainTbl").on("mouseleave"," span.oddsItem", function() {
        var index = $(this).closest("td").find(".oddsItem").index($(this));
        $("#headerTr span.oddsHeader:eq(" + index + ")").removeClass("headerOver");
    });
    //
});
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
        htmlStr += "<tr><td class='bDateTd' colspan='8' bIndex='" + filteAry.date.length + "'>周" + weekObj.cn + " " + tmpDate + " 共<label></label>场比赛 "
        if(matchNumDate !='' && matchNumDate != undefined){
            htmlStr += '<span class="match-date-num">(比赛编号日期：'+ matchNumDate +")</span>"
        }
        htmlStr += "<a href='javascript:void(0);' class='bDateHide'>[隐藏]</a>"
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
            //     weatherInfo = "<img title='" + tmpObj.weather + "' src='" + tmpObj.weather_pic + "' />";
            // }
            var disCls = "";
            if (tmpObj.ttg != undefined && tmpObj.ttg.cbt == "2") disCls = " oddsDis";
            var bgStr = "";
            var tmpNum = Math.round(Math.random() * 10); //测试数据 || tmpNum % 7 == 0
            if ((tmpObj.ttg.single == "1" && tmpObj.ttg.o_type == "F")) {
                bgStr = "background-image:url("+jsCommonDataV1.resDomain+"/res_1_0/jcw/images/jc_calculator/single.gif);background-repeat:no-repeat;background-position:left top;";
            }
            console.log('bg str ==== ', bgStr)
            var subscribeTeamImgUrl = "https://static.sporttery.cn/res_1_0/jcw/images/gz/qdmgz.png";
            var homeTeamImg = '<img src="' + subscribeTeamImgUrl + '" class="subscribe-icon img_' + tmpObj.h_id + '" onclick="stopEvent(event);zjqGz.subscribeTeam(\'' + tmpObj.h_id + '\', \'' + tmpObj.h_cn_abbr + '\', \'' + tmpObj.l_id + '\')">';
            var awayTeamImg = '<img src="' + subscribeTeamImgUrl + '" class="subscribe-icon img_' + tmpObj.a_id + '" onclick="stopEvent(event);zjqGz.subscribeTeam(\'' + tmpObj.a_id + '\', \'' + tmpObj.a_cn_abbr + '\', \'' + tmpObj.l_id + '\')">';
            htmlStr += "<tr id='list_" + tmpObj.id + "' dataIndex='" + i + "_" + j + "' class='listTr' lIndex='" + isFind + "' bIndex='" + filteAry.date.length
                + "' matchNumDate='" + tmpObj.match_num_date + "' taxDateNo='" + tmpObj.tax_date_no + "'>"
                + "<td" + ((i == 0) ? " style='width:43px;'" : "") + ">" + tmpObj.num.substr(0, 2) + "<br>" + tmpObj.num.substr(2) + "</td>"
                + "<td style=\"width: 70px\" title='" + tmpObj.l_cn + "' class='lname'>" + getTarget(tmpObj.id, tmpObj.l_cn_abbr, tmpObj.l_id, tmpObj.l_background_color) + "</td>"
                + "<td  style=\"width: 50px\">" + tmpObj.date.substr(5) + "<br>" + tmpObj.time.substr(0, 5) + "</td>"
                + "<td title='点击VS进入对阵数据页,点击球队名称进入球队介绍页' onclick='toArrData(" + tmpObj.id + ")' class='vsTd'"
                + "style='" + ((i == 0) ? "width:404px;" : "") + bgStr + "'>"
                + "<span class='match-left' style='width:164px'>"
                + homeTeamImg
                + "<a class=\"team-left\"  onclick='stopEvent(event)' href='//www.sporttery.cn/zqlszl/qdzl/index.html?gmtid=" + tmpObj.h_id + "' target='_blank'>"
            if(tmpObj.h_order !=''){
                htmlStr +=  "<label>" + tmpObj.h_order + "</label>"
            }
            htmlStr +="<span class=\"vs-left-padding\">" +tmpObj.h_cn_abbr + "</span>"+"</a>"
                +"</span>"
                + "<a onclick='stopEvent(event)' class='vsA' target='_blank' href='//www.sporttery.cn/jc/zqdz/index.html?showType=2&mid=" + tmpObj.id + "'> VS </a>"
                + "<span class='match-right' style='width:164px'>"
                + "<a class=\"team-right\" onclick='stopEvent(event)' href='//www.sporttery.cn/zqlszl/qdzl/index.html?gmtid=" + tmpObj.a_id + "' target='_blank'>"
                +"<span class=\"vs-right-padding\">" + tmpObj.a_cn_abbr + "</span></a>"
            if(tmpObj.a_order !=''){
                htmlStr +=  "<label>" + tmpObj.a_order + "</label>"
            }

            htmlStr +=  awayTeamImg
                +"</span>"
                +"</td>"
                + "<td class='ttgOdds'" + ((i == 0) ? " style='width:404px;'" : "") + ">"
                + "<span class='oddsItem oddsItemKeep" + disCls + "' style='border-left:none;width: 48px'>" + tmpObj.ttg.s0 + "</span>"
                + "<span class='oddsItem oddsItemKeep" + disCls + "'  style=\"width: 48px\">" + tmpObj.ttg.s1 + "</span>"
                + "<span class='oddsItem oddsItemKeep" + disCls + "'  style=\"width: 48px\">" + tmpObj.ttg.s2 + "</span>"
                + "<span class='oddsItem oddsItemKeep" + disCls + "'  style=\"width: 48px\">" + tmpObj.ttg.s3 + "</span>"
                + "<span class='oddsItem oddsItemKeep" + disCls + "'  style=\"width: 48px\">" + tmpObj.ttg.s4 + "</span>"
                + "<span class='oddsItem oddsItemKeep" + disCls + "'  style=\"width: 48px\">" + tmpObj.ttg.s5 + "</span>"
                + "<span class='oddsItem oddsItemKeep" + disCls + "'  style=\"width: 48px\">" + tmpObj.ttg.s6 + "</span>"
                + "<span class='oddsItem oddsItemKeep" + disCls + "'  style=\"width: 48px\">" + tmpObj.ttg.s7 + "</span>"
                + "</td>"
                + "<td  style=\"width: 72px\" class='linkTd'" + ((i == 0) ? " style='width:60px;'" : "") + "><a href='//www.sporttery.cn/jc/zqtjhc/?m=" + tmpObj.id + "' target='_blank'>同奖</a></td>"
                + "<td class='uOddsTd'  style=\"width: 120px\"><div class='ttgU'  style=\"width: 120px\"><span class='rLine'  style=\"width: 40px\">--</span><span class='rLine' style=\"width: 40px\">--</span><span style=\"width: 40px\">--</span></div></td>"
                +"<td style=\"width: 46px\"><img src=\"https://static.sporttery.cn/res_1_0/jcw/images/gz/scmgz.png\" id='img_" + tmpObj.id + "' class='subscribe-icon-sc img_" + tmpObj.id + "' style='cursor: pointer;' onclick=\"stopEvent(event);zjqGz.subscribeMatch('" + tmpObj.l_id + "', '" + tmpObj.id + "', '" + tmpObj.match_num + "')\"></td>"
                +"</tr>";
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
    //
    lotFunc.getReferDataI(0);
    zjqGz.showHtml = true
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
                    selItemStr += "<span title='" + tmpAry[i] + "' index='" + i + "'>" + oddsIndex[i] + "球</span>";
                }
            }
            str += "<tr id='sel_" + key + "'><td class='delSelLine'>×</td><td>" + dataObj.num + "</td><td>" + dataObj.h_cn_abbr + " VS " + dataObj.a_cn_abbr + "</td><td class='selItemTd'>" + selItemStr + "</td><td><input" + (($("#optionHeader input").attr("disabled") != undefined) ? " disabled" : "") + " type='checkbox'" + ((selObj.isDan) ? " checked" : "") + " class='danChk' /></td></tr>";
        }
    }
    $("#selDetailTbl").html(str);
    lotFunc.autoScroll();
    lotFunc.danCheck();
}
function getCopyData(param) {
    var str = getFixedLength(10, "赛事编号") + getFixedLength(10, "赛事") + getFixedLength(16, "开赛时间") + getFixedLength(24, "对阵") + getFixedLength(10, "0") + getFixedLength(10, "1") + getFixedLength(10, "2") + getFixedLength(10, "3") + getFixedLength(10, "4") + getFixedLength(10, "5") + getFixedLength(10, "6") + getFixedLength(10, "7+") + "\r\n";
    $("#mainTbl tr.listTr:visible").each(function() {
        var tdObj = $(this).find("td");
        var oddsObj = tdObj.eq(5).find("span");
        str += getFixedLength(10, tdObj.eq(0).text()) + getFixedLength(10, tdObj.eq(1).text()) + getFixedLength(16, tdObj.eq(2).html().toLocaleLowerCase().replace("<br>", " ")) + getFixedLength(24, tdObj.eq(4).text()) + getFixedLength(10, oddsObj.eq(0).text()) + getFixedLength(10, oddsObj.eq(1).text()) + getFixedLength(10, oddsObj.eq(2).text()) + getFixedLength(10, oddsObj.eq(3).text()) + getFixedLength(10, oddsObj.eq(4).text()) + getFixedLength(10, oddsObj.eq(5).text()) + getFixedLength(10, oddsObj.eq(6).text()) + getFixedLength(10, oddsObj.eq(7).text()) + "\r\n";
    });
    return str;
}

var zjqGz = {
    showHtml: false,
    canLoginTag: false,
    canOrderTag: false,
    teamList: [],
    matchList: [],
    refreshDebounceTimer: null,
    subscribeTeam: async function(teamId, teamName, leagueId) {
        if(!zjqGz.canLoginTag){
            goLoginFunPage();
            showLoginModal();
            return
        }
        if (zjqGz.refreshDebounceTimer) {
            return;
        }
        zjqGz.refreshDebounceTimer = setTimeout(function() {
            zjqGz.refreshDebounceTimer = null;
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
            zjqGz.setTrackEvent1('click_jingcaiFollowTeam_button',{'ext1':JSON.stringify(objTrack)})

            if (isSubscribe) {
                $(imgSelector).attr('src', subscribedImgUrl);
                const result = await teamSubscribeSubmitFun(zjqGz.scanChannel, '0', teamId, teamName, 0);
                console.log('subscribeTeam 结果:', result);
                if(result.errCode == 0) {
                    zjqGz.showToast('订阅关注成功')
                    getSubscribeStatus(result.data)
                } else if(result.errCode == '-20132602'){
                    $(imgSelector).attr('src', unsubscribedImgUrl);
                    zjqGz.initSubscribeStatus(zjqGz.teamList, 2);
                    zjqGz.initSubscribeMatchStatus(zjqGz.matchList, 2);
                    goLoginFunPage();
                    LoginModal.show();
                } else {
                    $(imgSelector).attr('src', unsubscribedImgUrl);
                    zjqGz.showToast(result.alert.actionData);
                }
            } else {
                $(imgSelector).attr('src', unsubscribedImgUrl);
                const result = await teamSubscribeCancelFun('0', teamId, 0);
                console.log('subscribeTeamCancel 结果:', result);
                if(result.errCode == 0) {
                    zjqGz.showToast('取消订阅关注成功');
                } else if(result.errCode == '-20132602'){
                    $(imgSelector).attr('src', subscribedImgUrl);
                    zjqGz.initSubscribeStatus(zjqGz.teamList, 2);
                    zjqGz.initSubscribeMatchStatus(zjqGz.matchList, 2);
                    goLoginFunPage();
                    LoginModal.show();
                } else {
                    $(imgSelector).attr('src', subscribedImgUrl);
                    zjqGz.showToast(result.alert.actionData);
                }
            }
        } catch (error) {
            console.error('subscribeTeam 错误:', error);
        }
    },
    refreshDebounceTimer2: null,
    subscribeMatch: async function(leagueId, matchId, matchNum) {
        if(!zjqGz.canLoginTag){
            goLoginFunPage();
            showLoginModal();
            return
        }
        if ( zjqGz.refreshDebounceTimer2) {
            return;
        }
        zjqGz.refreshDebounceTimer2 = setTimeout(function() {
            zjqGz.refreshDebounceTimer2 = null;
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
            zjqGz.setTrackEvent1('click_jingcaiFollowEvent_button',{'ext1':JSON.stringify(objTrack)})

            if (isSubscribe) {
                $(imgSelector).attr('src', subscribedImgUrl);
                const result = await matchSubscribeSubmitFun(zjqGz.scanChannel, '0', JSON.stringify([{leagueId:leagueId, matchId:+matchId, matchNum:+matchNum}]));
                console.log('subscribeMatch 结果:', result);
                if(result.errCode == 0) {
                    zjqGz.showToast('订阅关注成功')
                    getSubscribeStatus(result.data)
                } else if(result.errCode == '-20132602'){
                    $(imgSelector).attr('src', unsubscribedImgUrl);
                    zjqGz.initSubscribeStatus(zjqGz.teamList, 2);
                    zjqGz.initSubscribeMatchStatus(zjqGz.matchList, 2);
                    goLoginFunPage();
                    LoginModal.show();
                } else {
                    $(imgSelector).attr('src', unsubscribedImgUrl);
                    zjqGz.showToast(result.alert.actionData);
                }
            } else {
                $(imgSelector).attr('src', unsubscribedImgUrl);
                const result = await matchSubscribeCancelFun(zjqGz.scanChannel, '0', JSON.stringify([{leagueId:leagueId, matchId:+matchId, matchNum:+matchNum}]));
                console.log('subscribeMatchCancel 结果:', result);
                if(result.errCode == 0) {
                    zjqGz.showToast('取消订阅关注成功');
                } else if(result.errCode == '-20132602'){
                    $(imgSelector).attr('src', subscribedImgUrl);
                    zjqGz.initSubscribeStatus(zjqGz.teamList, 2);
                    zjqGz.initSubscribeMatchStatus(zjqGz.matchList, 2);
                    goLoginFunPage();
                    LoginModal.show();
                } else {
                    $(imgSelector).attr('src', subscribedImgUrl);
                    zjqGz.showToast(result.alert.actionData);
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
            zjqGz.canOrderTag = true
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
        if(!zjqGz.canLoginTag) {
            return
        }
        try {
            const result = await getSubscribeMatchListFun(
                zjqGz.scanChannel,
                '0',
                'current'
            );
            zjqGz.matchList = [];
            zjqGz.teamList = [];
            console.log('getSubscribeMatchListFun 结果:', result);
            if(result.errCode == 0) {
                if(result.data.matchList && result.data.matchList.length > 0){
                    result.data.matchList.forEach((element, index) => {
                        zjqGz.matchList.push(element.matchId)
                    })
                }
                if(result.data.teamList && result.data.teamList.length > 0){
                    result.data.teamList.forEach((element, index) => {
                        zjqGz.teamList.push(element.teamId)
                    })
                }
                zjqGz.initSubscribeStatus(zjqGz.teamList, 1);
                zjqGz.initSubscribeMatchStatus(zjqGz.matchList, 1);
            }
        } catch (error) {
            console.error('getSubscribeMatchListFun 错误:', error);
        }
    },
    showToast: function(message, duration) {
        duration = duration || 1500;

        var toastId = 'zjq_gz_toast_' + Date.now();
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
    zjqGz.loadSubscribedTeams();
});

function callListdata() {
    zjqGz.scanChannel = channelNameID[window.localStorage.getItem('getScanChannel') || '200000'] || 1;
    zjqGz.scanTokenTag = window.sessionStorage.getItem('getScanToken') || '';
    zjqGz.canLoginTag = true;

    if(zjqGz.showHtml){
        zjqGz.loadSubscribedTeams();
    } else {
        const checkBfGz = setInterval(() => {
            if (zjqGz.showHtml) {
                clearInterval(checkBfGz);
                zjqGz.loadSubscribedTeams();
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

