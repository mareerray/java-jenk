import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProfileComponent } from './profile.component';
import { UserService } from '../../services/user.service';
import { MediaService } from '../../services/media.service';
import { AuthService } from '../../services/auth.service';
import { HttpClient } from '@angular/common/http';
import { Router, ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

const mockUser = {
  id: 'user1',
  name: 'John Doe',
  email: 'john@example.com',
  role: 'CLIENT',
  avatar: 'https://example.com/avatar.jpg',
};

describe('ProfileComponent', () => {
  let component: ProfileComponent;
  let fixture: ComponentFixture<ProfileComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProfileComponent],
    })
      .overrideProvider(UserService, {
        useValue: {
          getCurrentUser: jasmine.createSpy('getCurrentUser').and.returnValue(of(mockUser)),
          updateCurrentUser: jasmine.createSpy('updateCurrentUser').and.returnValue(of(mockUser)),
        },
      })
      .overrideProvider(MediaService, {
        useValue: {
          uploadAvatar: jasmine
            .createSpy('uploadAvatar')
            .and.returnValue(of({ data: { url: 'new-avatar.jpg', id: 'media1' } })),
          deleteImage: jasmine.createSpy('deleteImage').and.returnValue(of({})),
          maxImageSize: 2 * 1024 * 1024,
          allowedAvatarTypes: ['image/jpeg', 'image/png'],
        },
      })
      .overrideProvider(AuthService, {
        useValue: {
          updateCurrentUserInStorage: jasmine.createSpy('updateCurrentUserInStorage'),
        },
      })
      .overrideProvider(HttpClient, {
        useValue: jasmine.createSpyObj('HttpClient', ['get', 'post']),
      })
      .overrideProvider(Router, {
        useValue: {
          createUrlTree: jasmine
            .createSpy('createUrlTree')
            .and.returnValue({ toString: () => '/profile' }),
          serializeUrl: jasmine.createSpy('serializeUrl').and.returnValue('/profile'),
          navigate: jasmine.createSpy('navigate'),
        },
      })
      .overrideProvider(ActivatedRoute, {
        useValue: { snapshot: { paramMap: { get: () => null } } },
      })
      .overrideProvider(HttpClient, {
        useValue: jasmine.createSpyObj('HttpClient', ['get', 'post']),
      })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ProfileComponent);
    component = fixture.componentInstance;
  });

  // ✅ 1. Component creates
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ✅ 2. Loads current user
  it('should load current user on init', async () => {
    fixture.detectChanges();
    expect(component.currentUser).toBeDefined();
    expect(component.currentUser?.name).toBe('John Doe');
    expect(component.profileForm.get('name')?.value).toBe('John Doe');
    expect(component.profileForm.get('email')?.disabled).toBeTrue();
  });

  // ✅ 3. Displays avatar
  it('should display user avatar', async () => {
    fixture.detectChanges();
    const avatarImg = fixture.nativeElement.querySelector('img.rounded-circle');
    expect(avatarImg.src).toContain('avatar.jpg');
  });

  // ✅ 4. Shows name + role
  it('should display user name and role', async () => {
    fixture.detectChanges();
    const nameElement = fixture.nativeElement.querySelector('.fw-bold.fs-2');
    expect(nameElement.textContent.trim()).toBe('John Doe');
    const roleBadge = fixture.nativeElement.querySelector('.badge.bg-primary');
    expect(roleBadge.textContent.trim()).toBe('CLIENT');
  });

  // ✅ 5. Save button disabled invalid
  it('should disable save button when form invalid', async () => {
    // 1. Load user → form VALID
    fixture.detectChanges(); // Triggers ngOnInit → loads user → patches form
    await fixture.whenStable(); // ← WAIT FOR ASYNC! User loads → form patches VALID data

    // 2. Clear form → INVALID
    component.profileForm.reset();
    fixture.detectChanges();

    // 3. Button disabled
    const saveButton = fixture.nativeElement.querySelector('button[type="submit"]');
    expect(saveButton.disabled).toBeTrue(); // ← NOW INVALID!
  });

  // ✅ 6. Success message shows
  it('should show success message when profile saved', async () => {
    fixture.detectChanges();
    await fixture.whenStable(); // Wait for user load

    // Fill form with valid data
    component.profileForm.setValue({ name: 'Jane Doe', email: 'john@example.com' });
    fixture.detectChanges();

    component.saveProfile();
    fixture.detectChanges(); // Trigger template update

    expect(component.successMessage).toContain('Profile updated successfully');
    expect(component.showSuccess).toBeTrue();
  });
});
