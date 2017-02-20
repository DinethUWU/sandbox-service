import {Component, OnInit} from '@angular/core';
import {ApiRemoteService} from "../../data-store/services/api-remote-service";
import {IAppState, IApiState} from "../../data-store/models/common-models";
import {Store} from "@ngrx/store";
import {Router, ActivatedRoute} from "@angular/router";
import {ApiActionCreatorService} from "../../data-store/actions/api-action-creator.service";

@Component({
    selector: 'api-main',
    templateUrl: './main.component.html',
    styleUrls: ['./main.component.scss']
})
export class ApiMainComponent implements OnInit {

    private apiTypes: string[];

    private apiIconMap = {
        location: 'location_on',
        sms: 'textsms',
        payment: 'attach_money',
        ussd: 'code',
        credit: 'credit_card',
        customerinfo: 'info_outline',
        wallet: 'account_balance_wallet',
        provisioning: 'cloud_upload'
    };

    constructor(private actionCreator: ApiActionCreatorService,
                private apiService: ApiRemoteService,
                private router: Router,
                private activatedRoute: ActivatedRoute,
                private store: Store<IAppState>) {

    }

    ngOnInit() {
        this.store.select('ApiData')
            .subscribe((apiData: IApiState) => {
                this.apiTypes = apiData.apiTypes;
            });

        this.apiService.getApiTypes();
    }

    onIconClick(type) {
        if (!!type) {
            this.actionCreator.setSelectedApiType(type);
            this.apiService.getApiServiceTypes(type);
            this.router.navigate([type.toLocaleLowerCase()], {relativeTo: this.activatedRoute});
        }
    }
}