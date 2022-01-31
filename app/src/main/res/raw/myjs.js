window.addEventListener('scroll', function () {
    var links = document.querySelectorAll('a:not([target=_blank])');
    for (var i = 0; i < links.length; i++) {
        links[i].target = '_blank';
    }
});