import {UiParams} from "../utils/UiParams.js?version=@version@";

export class HomeUi {

    constructor() {
        console.info("Starting home UI");
        this.noFilterButton = $('#noFilterButton');
        this.workflowFilterButton = $('#workflowFilterButton');
        this.formFilterButton = $('#formFilterButton');
        this.globalFilterButton = $('#globalFilterButton');
        this.workflowFilterStatus = true;
        this.formFilterStatus = true;
        this.globalFilterStatus = true;
        this.menuToggled = false;
        this.uiParams = new UiParams();
        this.initListeners();
        if(localStorage.getItem('menuToggled') === "true") {
            this.toggleNewMenu();
        }
    }

    initListeners() {
        $('#toggleNewGrid').on('click', e => this.toggleNewMenu());
        $('#newScroll').on('mousewheel DOMMouseScroll', e => this.activeHorizontalScrolling(e));
        this.noFilterButton.on('click', e => this.showAll(e));
        this.workflowFilterButton.on('click', e => this.filterWorkflows(e));
        this.globalFilterButton.on('click', e => this.filterGlobal(e));
        this.formFilterButton.on('click', e => this.filterForms(e));
        $('[id^="deleteWorkflow_"]').each(function (){
            $(this).on('submit', function (e){
                e.preventDefault();
                let target = e.currentTarget;
                bootbox.confirm("Voulez-vous vraiment supprimer ce circuit ?", function (result) {
                    if(result) {
                        target.submit();
                    }
                });
            });
        });
    }

    toggleNewMenu() {
        console.info("toggle new menu");
        $('#newScroll').toggleClass('text-nowrap').toggleClass('new-min-h');
        $('#toSignList').toggleClass('d-flex d-none');
        $('#toggleNewGrid').children().toggleClass('fa-th fa-chevron-up');
        $('.newHr').toggleClass('d-none');
        $('#newContainer').toggleClass('d-inline').toggleClass("text-left");
        $('.newToggled').toggleClass('d-none');
        $('.noForm').toggleClass('d-none');
        $('.noWorkflow').toggleClass('d-none');
        this.menuToggled = !this.menuToggled;
        localStorage.setItem('menuToggled', this.menuToggled);
    }

    hideAll() {
        $('.globalButton').addClass('d-none');
        $('.workflow-button').addClass('d-none');
        $('.formButton').addClass('d-none');
        this.noFilterButton.removeClass('btn-secondary');
        this.noFilterButton.addClass('btn-light');
        this.globalFilterButton.removeClass('btn-secondary');
        this.workflowFilterButton.removeClass('btn-secondary');
        this.formFilterButton.removeClass('btn-secondary');
        this.globalFilterButton.addClass('btn-light');
        this.workflowFilterButton.addClass('btn-light');
        this.formFilterButton.addClass('btn-light');
    }

    showAll() {
        $('.globalButton').removeClass('d-none');
        $('.workflow-button').removeClass('d-none');
        $('.formButton').removeClass('d-none');
        this.noFilterButton.addClass('btn-secondary');
        this.noFilterButton.removeClass('btn-light');
        this.globalFilterButton.removeClass('btn-secondary');
        this.workflowFilterButton.removeClass('btn-secondary');
        this.formFilterButton.removeClass('btn-secondary');
        this.globalFilterButton.addClass('btn-light');
        this.workflowFilterButton.addClass('btn-light');
        this.formFilterButton.addClass('btn-light');
    }



    filterGlobal(e) {
        this.hideAll();
        this.globalFilterButton.removeClass('btn-light');
        this.globalFilterButton.addClass('btn-secondary');
        $('.globalButton').removeClass('d-none');
        this.globalFilterStatus = !this.globalFilterStatus;
        return this.uiParams.set("globalFilterStatus", this.globalFilterStatus);
    }

    filterWorkflows(e) {
        this.hideAll();
        this.workflowFilterButton.removeClass('btn-light');
        this.workflowFilterButton.addClass('btn-secondary');
        $('.workflow-button').removeClass('d-none');
        this.workflowFilterStatus = !this.workflowFilterStatus;
        return this.uiParams.set("workflowFilterStatus", this.workflowFilterStatus);
    }

    filterForms(e) {
        this.hideAll();
        this.formFilterButton.removeClass('btn-light');
        this.formFilterButton.addClass('btn-secondary');
        $('.formButton').removeClass('d-none');
        this.formFilterStatus = !this.formFilterStatus;
        return this.uiParams.set("formFilterStatus", this.formFilterStatus);
    }

    activeHorizontalScrolling(e){
        if(!this.menuToggled) {
            let delta = Math.max(-1, Math.min(1, (e.originalEvent.wheelDelta || -e.originalEvent.detail)));
            $(e.currentTarget).scrollLeft($(e.currentTarget).scrollLeft() - ( delta * 40 ) );
            e.preventDefault();
        }
    }

}