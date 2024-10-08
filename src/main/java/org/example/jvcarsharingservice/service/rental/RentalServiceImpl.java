package org.example.jvcarsharingservice.service.rental;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.jvcarsharingservice.dto.rental.CreateRentalRequestDto;
import org.example.jvcarsharingservice.dto.rental.RentalDto;
import org.example.jvcarsharingservice.dto.rental.RentalSearchParameters;
import org.example.jvcarsharingservice.mapper.RentalMapper;
import org.example.jvcarsharingservice.model.classes.Car;
import org.example.jvcarsharingservice.model.classes.Rental;
import org.example.jvcarsharingservice.model.classes.User;
import org.example.jvcarsharingservice.repository.car.CarRepository;
import org.example.jvcarsharingservice.repository.rental.RentalRepository;
import org.example.jvcarsharingservice.repository.rental.provider.RentalSpecificationBuilder;
import org.example.jvcarsharingservice.service.notification.NotificationService;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RentalServiceImpl implements RentalService {
    private final RentalRepository rentalRepository;
    private final RentalMapper rentalMapper;
    private final CarRepository carRepository;
    private final RentalSpecificationBuilder rentalSpecificationBuilder;
    private final NotificationService notificationService;
    
    @Override
    @Transactional
    public RentalDto addRental(
            User user, CreateRentalRequestDto createRentalRequestDto) {
        updateCarInventoryAfterRent(createRentalRequestDto);
        notificationService.notifyNewRentalsCreated(
                "User with id " + user.getId()
                + " rent a car with id " + createRentalRequestDto.carId()
                + " from " + createRentalRequestDto.rentalDate()
                + " to " + createRentalRequestDto.returnDate()
        );
        Rental entity = rentalMapper.toEntity(createRentalRequestDto);
        entity.setUserId(user.getId());
        return rentalMapper.toDto(
                rentalRepository.save(entity)
        );
    }

    private void updateCarInventoryAfterRent(CreateRentalRequestDto createRentalRequestDto) {
        Car car = carRepository.findById(createRentalRequestDto.carId()).orElseThrow(
                () -> new EntityNotFoundException(
                        "Car with id " + createRentalRequestDto.carId() + " not found")
        );
        car.setInventory(car.getInventory() - 1);
    }

    @Override
    public List<RentalDto> getRentals(RentalSearchParameters rentalSearchParameters) {
        Specification<Rental> build = rentalSpecificationBuilder.build(rentalSearchParameters);
        return rentalRepository.findAll(build).stream()
                .map(rentalMapper::toDto)
                .toList();
    }

    @Override
    public RentalDto getRental(Long id) {
        return rentalMapper.toDto(
                rentalRepository.findById(id).orElseThrow(
                        () -> new EntityNotFoundException("Rental with id " + id + " not found")
                )
        );
    }

    @Override
    public void returnRental(User user, Long id) {
        Rental rental = rentalRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Rental with id " + id + " not found")
        );
        checkIfThisRentIsForCorrectUser(user, rental);

        rental.setActualReturnDate(LocalDate.now());
        notificationForOverdueRent(rental);

        rental.setId(id);
        Long carId = rental.getCarId();
        Car car = carRepository.findById(carId).orElseThrow(
                () -> new EntityNotFoundException("Car with id " + carId + " not found")
        );
        car.setInventory(car.getInventory() + 1);

        rentalRepository.save(rental);
    }

    private void notificationForOverdueRent(Rental rental) {
        if (rental.getReturnDate().isAfter(LocalDate.now())) {
            notificationService.notifyOverdueRentals(
                    "user with id " + rental.getUserId()
                    + " returns the car with a delay, car id " + rental.getCarId()
            );
        }
    }

    private static void checkIfThisRentIsForCorrectUser(User user, Rental rental) {
        if (!user.getId().equals(rental.getUserId())) {
            throw new EntityNotFoundException(
                    "User with id " + user.getId()
                    + " does not belong to this Rental"
            );
        }
    }
}
