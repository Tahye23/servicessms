import { Routes } from '@angular/router';

import { UserRouteAccessService } from 'app/core/auth/user-route-access.service';
import { ASC } from 'app/config/navigation.constants';
import { GroupeComponent } from './list/groupe.component';
import { GroupeDetailComponent } from './detail/groupe-detail.component';
import { GroupeUpdateComponent } from './update/groupe-update.component';
import GroupeResolve from './route/groupe-routing-resolve.service';

const groupeRoute: Routes = [
  {
    path: '',
    component: GroupeComponent,
    data: {
      defaultSort: 'id,' + ASC,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/view',
    component: GroupeDetailComponent,
    resolve: {
      groupe: GroupeResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: 'new',
    component: GroupeUpdateComponent,
    resolve: {
      groupe: GroupeResolve,
    },
    canActivate: [UserRouteAccessService],
  },
  {
    path: ':id/edit',
    component: GroupeUpdateComponent,
    resolve: {
      groupe: GroupeResolve,
    },
    canActivate: [UserRouteAccessService],
  },
];

export default groupeRoute;
