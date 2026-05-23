(function () {
    const coarsePointer = window.matchMedia?.('(pointer: coarse)').matches;
    if (!coarsePointer) return;

    document.documentElement.classList.add('touch-device');

    function prepareBoard(board) {
        if (!board) return;

        board.addEventListener('selectstart', event => event.preventDefault());
        board.addEventListener('contextmenu', event => event.preventDefault());
        board.addEventListener('dragstart', event => event.preventDefault());

        board.querySelectorAll('.cell, .cell > span').forEach(element => {
            element.setAttribute('draggable', 'false');
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => prepareBoard(document.getElementById('board')));
    } else {
        prepareBoard(document.getElementById('board'));
    }
})();
