import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { TokensAppComponent } from './list/tokens-app.component';
import { TokensAppDetailComponent } from './detail/tokens-app-detail.component';
import { TokensAppUpdateComponent } from './update/tokens-app-update.component';
import TokensAppResolve from './route/tokens-app-routing-resolve.service';

const tokensAppRoute: Routes = [
  {
    path: '',
    component: TokensAppComponent,
    data: {},
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: TokensAppDetailComponent,
    resolve: {
      tokensApp: TokensAppResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: TokensAppUpdateComponent,
    resolve: {
      tokensApp: TokensAppResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: TokensAppUpdateComponent,
    resolve: {
      tokensApp: TokensAppResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default tokensAppRoute;
