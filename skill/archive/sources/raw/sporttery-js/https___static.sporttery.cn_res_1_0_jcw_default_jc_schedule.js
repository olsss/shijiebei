
/*表格隔行换色*/
$(function () {
    $(".m-tab tr:odd").addClass("odd");
    $(".m-tab tr:even").addClass("even");
});

/*表格头部跟着页面悬浮*/
var listTop;
$(document).ready(function () {
    listTop = $("#headerTr").offset().top;
    $(window).scroll(scrollHead);

});
function scrollHead() {
    if (listTop < 100) { listTop = $("#headerTr").offset().top; }
    var sTop = $(document).scrollTop();
    $("#headerTr").css("position", "relative");
    if (sTop > listTop) {
        $("#headerTr").stop();
        $("#headerTr").animate({ "top": "+=" + (sTop - $("#headerTr").offset().top) + "px" }, "slow", function () { });
    }
    else {
        $("#headerTr").stop();
        $("#headerTr").css("top", 0);
    }
}
$(document).ready(function () {
    /*赛事筛选展开*/
    $(document).on("click", "#u-sel", function () {
        $("#filterTbl").show();
    });
    $(document).on("click", ".u-close", function () {
        $("#filterTbl").hide();
    });
    $(document).on("click", "#filterHeader", function () {
        $("#filterTbl").hide();
    });

    /*胜平负展开*/
    $(document).on("click", ".u-em", function () {
        if ($('.m-spf').is(':hidden')) {
            $(this).addClass("u-em2")
            $('.m-spf').show();
        }
        else {
            $(this).removeClass("u-em2")
            $('.m-spf').hide();
        }
    });

    /*设置展开*/
    $(document).on("click", ".u-setup", function () {
        if ($('.m-sz').is(':hidden')) {
            $('.m-sz').show();
        }
        else {
            $('.m-sz').hide();
        }
    });
    /*喇叭开关*/
    $('.u-quite').toggle(function () {
        $(this).addClass('u-noquite');
    }, function () {
        $(this).removeClass('u-noquite');
    });
    $(document).ready(function () {
        /*选中赛事保留/隐藏弹出层*/
        $("#m-tab1 input").click(function () {
            $(".m-select").css({ left: $(window).width() / 2 - 620 + $(window).scrollLeft() });
            $(".m-select").show();
        });
        $(".m-select").click(function () {
            $(".m-select").hide();
        });
    })

});

