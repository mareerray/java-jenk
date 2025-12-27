import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SignUpComponent } from './sign-up.component';
import { AuthService } from '../../../services/auth.service';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { of } from 'rxjs';

describe('SignUpComponent', () => {
  let component: SignUpComponent;
  let fixture: ComponentFixture<SignUpComponent>;
  let mockAuthService: jasmine.SpyObj<AuthService>;
  let mockRouter: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    mockAuthService = jasmine.createSpyObj('AuthService', ['signup']);
    mockRouter = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree']);

    await TestBed.configureTestingModule({
      imports: [
        SignUpComponent,
        MatCardModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatIconModule,
        MatProgressSpinnerModule,
        MatRadioModule,
      ],
    })
      .overrideProvider(AuthService, { useValue: mockAuthService })
      .overrideProvider(Router, {
        useValue: {
          ...mockRouter,
          createUrlTree: jasmine
            .createSpy('createUrlTree')
            .and.returnValue({ toString: () => '/signin' }),
          serializeUrl: jasmine.createSpy('serializeUrl').and.returnValue('/signin'),
          navigate: jasmine.createSpy('navigate'),
          events: of(), // Router events observable
          url: '/signup', // Current URL
        },
      })
      .overrideProvider(ActivatedRoute, {
        useValue: { snapshot: { paramMap: { get: () => null } } },
      })
      .overrideProvider(HttpClient, { useValue: jasmine.createSpyObj('HttpClient', ['post']) })
      .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(SignUpComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ✅ 1. Component creates
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ✅ 2. Default avatar displays
  it('should display default avatar', () => {
    const avatarImg = fixture.nativeElement.querySelector('.avatar-preview');
    expect(avatarImg.src).toContain('user-default.png');
  });

  // ✅ 3. Error messages show
  it('should show error message when present', () => {
    component.errorMessage = 'Email already exists';
    fixture.detectChanges();
    const errorAlert = fixture.nativeElement.querySelector('.alert.alert-danger');
    expect(errorAlert.textContent).toContain('Email already exists');
  });

  // ✅ 4. Submit disabled when invalid
  it('should disable submit button when form invalid', () => {
    const submitButton = fixture.nativeElement.querySelector('button[type="submit"]');
    expect(submitButton.disabled).toBeTrue();
  });

  // ✅ 5. Loading state works
  it('should enable submit button and show spinner when loading', () => {
    component.form.setValue({
      name: 'John Doe',
      email: 'john@example.com',
      password: 'Password123',
      confirmPassword: 'Password123',
      role: 'client',
    });
    component.isLoading = true;
    fixture.detectChanges();

    const submitButton = fixture.nativeElement.querySelector('button[type="submit"]');
    expect(submitButton.disabled).toBeTrue();
    expect(submitButton.textContent).toContain('Creating Account...');
  });
});
