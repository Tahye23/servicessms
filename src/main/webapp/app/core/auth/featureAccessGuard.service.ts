import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { map, switchMap, catchError } from 'rxjs/operators';
import { SubscriptionService } from '../../Subscription/service/subscriptionService.service';
import { DynamicMenuService } from '../../Subscription/service/dynamicMenuService.service';
import { AccountService } from 'app/core/auth/account.service';
import { of } from 'rxjs';

export const FeatureAccessGuard: CanActivateFn = (route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
  const subscriptionService = inject(SubscriptionService);
  const menuService = inject(DynamicMenuService);
  const router = inject(Router);
  const accountService = inject(AccountService);

  const requiredFeature = route.data['feature'] as string;
  const requiredPermission = route.data['permission'] as string;

  // Si aucune feature/permission requise, autoriser l'accès
  if (!requiredFeature && !requiredPermission) {
    return true;
  }

  return accountService.identity().pipe(
    switchMap(account => {
      if (!account) {
        // Utilisateur non connecté, rediriger vers login
        router.navigate(['/login'], {
          queryParams: { returnUrl: state.url },
        });
        return of(false);
      }

      // Admin : accès illimité
      if (account.authorities?.includes('ROLE_ADMIN')) {
        return of(true);
      }

      // Vérifier les permissions utilisateur
      if (requiredPermission && !hasUserPermission(account, requiredPermission)) {
        router.navigate(['/upgrade'], {
          queryParams: {
            feature: requiredPermission,
            reason: 'permission_required',
            message: 'Permission requise pour accéder à cette fonctionnalité',
            returnUrl: state.url,
          },
        });
        return of(false);
      }

      // Vérifier l'accès à la fonctionnalité via abonnement
      if (requiredFeature) {
        return subscriptionService.loadUserSubscriptions().pipe(
          map(() => {
            // Double vérification : permission + abonnement
            const hasFeatureAccess = menuService.hasFeatureAccess(requiredFeature);

            if (!hasFeatureAccess) {
              const access = subscriptionService.subscriptionAccess();
              const reason = getAccessDeniedReason(requiredFeature, access, account);

              router.navigate(['/upgrade'], {
                queryParams: {
                  feature: requiredFeature,
                  reason: reason.code,
                  message: reason.message,
                  returnUrl: state.url,
                },
              });
              return false;
            }

            return true;
          }),
          catchError(error => {
            console.error("Erreur lors de la vérification d'accès:", error);
            router.navigate(['/upgrade'], {
              queryParams: {
                feature: requiredFeature,
                reason: 'error',
                message: "Erreur lors de la vérification des droits d'accès",
                returnUrl: state.url,
              },
            });
            return of(false);
          }),
        );
      }

      return of(true);
    }),
    catchError(error => {
      console.error('Erreur dans FeatureAccessGuard:', error);
      router.navigate(['/upgrade'], {
        queryParams: {
          reason: 'error',
          message: "Erreur d'authentification",
          returnUrl: state.url,
        },
      });
      return of(false);
    }),
  );
};

/**
 * Vérifier si l'utilisateur a une permission spécifique
 */
function hasUserPermission(account: any, permission: string): boolean {
  if (!account.permissions) return false;

  try {
    const permissions = JSON.parse(account.permissions);
    return permissions.includes(permission);
  } catch {
    return false;
  }
}

/**
 * Déterminer la raison du refus d'accès
 */
function getAccessDeniedReason(feature: string, access: any, account: any): { code: string; message: string } {
  const permission = getRequiredPermissionForFeature(feature);

  // Vérifier d'abord les permissions
  if (permission && !hasUserPermission(account, permission)) {
    return {
      code: 'permission_denied',
      message: `Permission "${permission}" requise pour accéder à cette fonctionnalité`,
    };
  }

  // Ensuite vérifier l'abonnement
  switch (feature) {
    case 'send-sms':
      if (!access.canSendSMS) {
        return {
          code: 'subscription_required',
          message: 'Abonnement SMS requis',
        };
      }
      if (access.smsRemaining <= 0) {
        return {
          code: 'no_credits',
          message: 'Plus de crédits SMS disponibles',
        };
      }
      break;

    case 'send-whatsapp':
      if (!access.canSendWhatsApp) {
        return {
          code: 'subscription_required',
          message: 'Abonnement WhatsApp requis',
        };
      }
      if (access.whatsappRemaining <= 0) {
        return {
          code: 'no_credits',
          message: 'Plus de crédits WhatsApp disponibles',
        };
      }
      break;

    case 'templates':
      if (!access.canManageTemplates) {
        return {
          code: 'subscription_required',
          message: 'Abonnement premium requis pour gérer les modèles',
        };
      }
      break;

    case 'manage-users':
      if (!access.canManageUsers) {
        return {
          code: 'subscription_required',
          message: 'Abonnement premium requis pour gérer les utilisateurs',
        };
      }
      break;

    case 'conversations':
      if (!access.canViewConversations) {
        return {
          code: 'subscription_required',
          message: 'Abonnement premium requis pour voir les conversations',
        };
      }
      break;

    case 'api':
      if (!access.canManageAPI) {
        return {
          code: 'subscription_required',
          message: 'Abonnement premium requis pour gérer les APIs',
        };
      }
      break;

    default:
      return {
        code: 'access_denied',
        message: 'Accès refusé à cette fonctionnalité',
      };
  }

  return {
    code: 'unknown',
    message: 'Raison inconnue',
  };
}

/**
 * Mapper les features vers les permissions requises
 */
function getRequiredPermissionForFeature(feature: string): string | null {
  const featurePermissionMap: { [key: string]: string } = {
    'send-sms': 'sms',
    'send-whatsapp': 'whatsapp',
    templates: 'templates',
    'manage-users': 'users',
    conversations: 'conversations',
    contacts: 'contacts',
    groups: 'groups',
    api: 'applications',
    'view-dashboard': 'dashboard',
  };

  return featurePermissionMap[feature] || null;
}
